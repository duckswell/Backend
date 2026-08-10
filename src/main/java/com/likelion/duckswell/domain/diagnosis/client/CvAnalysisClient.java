package com.likelion.duckswell.domain.diagnosis.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 외부 duckswellAI check_photo.py / analyze.py를 서브프로세스로 호출하는 클라이언트.
 * 두 스크립트 모두 stdout에 JSON 한 줄을 출력하고, mediapipe/protobuf 로그는 stderr로만 나감.
 */
@Slf4j
@Component
public class CvAnalysisClient {

    private static final int TIMEOUT_SECONDS = 30;

    private final String pythonPath;
    private final String scriptsPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CvAnalysisClient(
            @Value("${duckswell-ai.python-path:}") String pythonPath,
            @Value("${duckswell-ai.scripts-path:}") String scriptsPath
    ) {
        this.pythonPath = pythonPath;
        this.scriptsPath = scriptsPath;
    }

    public PhotoQualityResult checkPhotoQuality(String imagePath) {
        JsonNode json = runScript("check_photo.py", imagePath);
        boolean ok = json.path("ok").asBoolean(false);
        String reason = json.hasNonNull("reason") ? json.get("reason").asText() : null;
        return new PhotoQualityResult(ok, reason);
    }

    public CvScoreResult analyze(String imagePath) {
        JsonNode json = runScript("analyze.py", imagePath);
        if (json.has("error")) {
            log.warn("analyze.py 분석 실패: {}", json.get("error").asText());
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        }
        if (!json.hasNonNull("redness_pct") || !json.hasNonNull("blemish_pct") || !json.hasNonNull("texture_pct")) {
            log.warn("analyze.py 응답에 필수 점수 필드가 없습니다: {}", json);
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        }
        return new CvScoreResult(
                json.path("redness_pct").asDouble(),
                json.path("blemish_pct").asDouble(),
                json.path("texture_pct").asDouble()
        );
    }

    private JsonNode runScript(String scriptName, String imagePath) {
        if (pythonPath.isBlank() || scriptsPath.isBlank()) {
            log.error("duckswell-ai.python-path / scripts-path 설정이 없습니다. application-local.yml 또는 "
                    + "application-prod.yml에 duckswellAI 경로를 채워주세요.");
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptsPath + "/" + scriptName, imagePath);
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        // mediapipe/protobuf가 stderr에 로그를 많이 찍는데 아무도 안 읽으면 파이프 버퍼가 차서
        // 자식 프로세스가 write에서 멈추고, 부모는 stdout 읽기에서 같이 멈추는 교착이 생긴다.
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = null;
        try {
            process = processBuilder.start();
            Process startedProcess = process;
            // stdout을 별도 스레드에서 읽어야 waitFor의 타임아웃이 실제로 의미가 있다.
            // (읽기를 먼저 blocking으로 하면 타임아웃 이전에 여기서 무한 대기할 수 있음)
            CompletableFuture<byte[]> stdoutFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return startedProcess.getInputStream().readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                log.error("{} 타임아웃({}s)", scriptName, TIMEOUT_SECONDS);
                throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
            }

            String stdout = new String(stdoutFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), StandardCharsets.UTF_8);
            return objectMapper.readTree(stdout);
        } catch (IOException | UncheckedIOException | ExecutionException | TimeoutException e) {
            log.error("{} 실행 실패", scriptName, e);
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
