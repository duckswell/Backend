package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LlmRecoveryStageClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String systemPrompt;

    public LlmRecoveryStageClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = loadSystemPrompt();
    }

    public LlmRecoveryStageResult summarize(LlmRecoveryStageContext context) {
        if (apiKey.isBlank() || model.isBlank()) {
            log.error("openai.api-key / openai.model 설정이 없습니다. application-local.yml 또는 "
                    + "application-prod.yml에 채워주세요.");
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }

        ObjectNode requestBody = buildRequestBody(context);

        String responseBody;
        try {
            byte[] requestBytes = objectMapper.writeValueAsBytes(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETIONS_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBytes))
                    .build();
            HttpResponse<byte[]> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            responseBody = new String(httpResponse.body(), StandardCharsets.UTF_8);

            if (httpResponse.statusCode() >= 400) {
                log.error("OpenAI 호출 실패: status={}, body={}", httpResponse.statusCode(), responseBody);
                throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
            }
        } catch (IOException | InterruptedException e) {
            log.error("OpenAI 호출 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }

        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (IOException e) {
            log.error("OpenAI 응답 JSON 파싱 실패: {}", responseBody, e);
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }

        return parseResult(response);
    }

    private ObjectNode buildRequestBody(LlmRecoveryStageContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.set("messages", buildMessages(context));
        root.set("response_format", buildResponseFormat());
        return root;
    }

    private ArrayNode buildMessages(LlmRecoveryStageContext context) {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", buildUserText(context));
        messages.add(userMessage);

        return messages;
    }

    private String buildUserText(LlmRecoveryStageContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[회복 일차] ").append(context.dayNumber()).append("일차\n");

        if (!context.hasYesterday()) {
            sb.append("[어제 관리 기록] 없음(오늘 회복 단계만 서술)\n");
            return sb.toString();
        }

        sb.append("[어제 관리 기록] 있음\n");
        sb.append("[어제 기록된 증상] ")
                .append(context.yesterdaySymptoms().isEmpty()
                        ? "없음"
                        : context.yesterdaySymptoms().stream().map(Symptom::name).collect(Collectors.joining(", ")))
                .append('\n');
        if (context.yesterdaySymptomNote() != null && !context.yesterdaySymptomNote().isBlank()) {
            sb.append("[어제 증상 메모] ").append(context.yesterdaySymptomNote()).append('\n');
        }
        sb.append("[어제 루틴 완료 요약] ").append(context.yesterdayCompletionSummaryText()).append('\n');

        return sb.toString();
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "recovery_stage");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", buildJsonSchema());
        responseFormat.set("json_schema", jsonSchema);

        return responseFormat;
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("recovery_stage_summary_text", objectMapper.createObjectNode().put("type", "string"));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.set("properties", rootProperties);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("recovery_stage_summary_text");
        root.set("required", required);
        root.put("additionalProperties", false);
        return root;
    }

    private LlmRecoveryStageResult parseResult(JsonNode response) {
        try {
            String content = response.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);
            return new LlmRecoveryStageResult(parsed.get("recovery_stage_summary_text").asText());
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패: {}", response, e);
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }
    }

    private String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/recovery-stage-system.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
