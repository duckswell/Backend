package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.global.client.llm.OpenAiChatClient;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LlmRecoveryStageClient {

    private final OpenAiChatClient openAiChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String systemPrompt;

    public LlmRecoveryStageClient(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.systemPrompt = loadSystemPrompt();
    }

    public LlmRecoveryStageResult summarize(LlmRecoveryStageContext context) {
        ObjectNode requestBody = buildRequestBody(context);
        JsonNode response = openAiChatClient.call(requestBody, DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        return parseResult(response);
    }

    private ObjectNode buildRequestBody(LlmRecoveryStageContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", openAiChatClient.model());
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
                .append(context.yesterdaySymptoms().isEmpty() || context.yesterdaySymptoms().contains(Symptom.NONE)
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
