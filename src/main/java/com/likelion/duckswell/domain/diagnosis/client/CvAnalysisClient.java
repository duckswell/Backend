package com.likelion.duckswell.domain.diagnosis.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
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
        return new CvScoreResult(
                json.get("redness_pct").asDouble(),
                json.get("blemish_pct").asDouble(),
                json.get("texture_pct").asDouble()
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

        try {
            Process process = processBuilder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
            }
            return objectMapper.readTree(stdout);
        } catch (IOException e) {
            log.error("{} 실행 실패", scriptName, e);
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED);
        }
    }
}
