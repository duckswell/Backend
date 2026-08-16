package com.likelion.duckswell.global.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.global.exception.CustomException;
import com.likelion.duckswell.global.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI를 호출하는 모든 도메인 클라이언트가 공유하는 Chat Completions 전송부.
 * 기본 키로 호출해 429(rate limit)를 받으면, api-key-fallback이 설정된 경우에만 보조 키로 한 번 더 재시도한다.
 * api-key-fallback이 비어 있으면(기본값) 기존과 동일하게 즉시 실패한다.
 * 실패 시 던질 에러코드는 호출부(도메인)가 결정하도록 인자로 받는다.
 */
@Slf4j
@Component
public class OpenAiChatClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int RATE_LIMIT_STATUS = 429;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String fallbackApiKey;
    private final String model;
    /** 앱 프로세스 시작 이후 누적 "성공" 호출 횟수 - rate limit 등으로 거부된 시도는 세지 않는다(재시작 시 0부터 다시 셈). */
    private final AtomicInteger callSuccessCount = new AtomicInteger(0);

    public OpenAiChatClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.api-key-fallback:}") String fallbackApiKey,
            @Value("${openai.model:}") String model
    ) {
        this.apiKey = apiKey;
        this.fallbackApiKey = fallbackApiKey;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public JsonNode call(ObjectNode requestBody, ErrorCode errorCode) {
        if (apiKey.isBlank() || model.isBlank()) {
            log.error("openai.api-key / openai.model 설정이 없습니다. application-local.yml 또는 "
                    + "application-prod.yml에 채워주세요.");
            throw new CustomException(errorCode);
        }

        byte[] requestBytes;
        try {
            requestBytes = objectMapper.writeValueAsBytes(requestBody);
        } catch (IOException e) {
            log.error("OpenAI 요청 직렬화 실패", e);
            throw new CustomException(errorCode);
        }

        String responseBody = send(requestBytes, apiKey, errorCode);
        if (responseBody == null && !fallbackApiKey.isBlank()) {
            log.warn("기본 OpenAI 키가 rate limit에 걸려 보조 키로 재시도합니다.");
            responseBody = send(requestBytes, fallbackApiKey, errorCode);
        }
        if (responseBody == null) {
            throw new CustomException(errorCode);
        }

        try {
            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            log.error("OpenAI 응답 JSON 파싱 실패: {}", responseBody, e);
            throw new CustomException(errorCode);
        }
    }

    /**
     * 429(rate limit)면 null을 반환해 호출부가 보조 키로 재시도할지 판단하게 하고,
     * 그 외 실패(4xx/5xx/네트워크 오류)는 재시도 대상이 아니므로 바로 예외를 던진다.
     */
    private String send(byte[] requestBytes, String key, ErrorCode errorCode) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETIONS_URL))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBytes))
                    .build();
            HttpResponse<byte[]> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String responseBody = new String(httpResponse.body(), StandardCharsets.UTF_8);

            if (httpResponse.statusCode() == RATE_LIMIT_STATUS) {
                log.error("OpenAI 호출 실패(rate limit): status={}, body={}", httpResponse.statusCode(), responseBody);
                return null;
            }
            if (httpResponse.statusCode() >= 400) {
                log.error("OpenAI 호출 실패: status={}, body={}", httpResponse.statusCode(), responseBody);
                throw new CustomException(errorCode);
            }
            log.info("OpenAI API 호출 성공 (누적 {}번째)", callSuccessCount.incrementAndGet());
            return responseBody;
        } catch (IOException | InterruptedException e) {
            log.error("OpenAI 호출 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(errorCode);
        }
    }
}
