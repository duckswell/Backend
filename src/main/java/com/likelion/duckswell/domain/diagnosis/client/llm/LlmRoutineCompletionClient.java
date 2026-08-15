package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineCompletionContext.CompletedStep;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.global.client.llm.OpenAiChatClient;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LlmRoutineCompletionClient {

    private final OpenAiChatClient openAiChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String systemPrompt;

    public LlmRoutineCompletionClient(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.systemPrompt = loadSystemPrompt();
    }

    public LlmRoutineCompletionResult summarize(LlmRoutineCompletionContext context) {
        ObjectNode requestBody = buildRequestBody(context);
        JsonNode response = openAiChatClient.call(requestBody, DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        return parseResult(response);
    }

    private ObjectNode buildRequestBody(LlmRoutineCompletionContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", openAiChatClient.model());
        root.set("messages", buildMessages(context));
        root.set("response_format", buildResponseFormat());
        return root;
    }

    private ArrayNode buildMessages(LlmRoutineCompletionContext context) {
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

    private String buildUserText(LlmRoutineCompletionContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[코스 타입] ").append(context.courseType().name()).append('\n');
        if (context.courseType() == CourseType.DAILY) {
            sb.append("[현재 관리 타입] ").append(context.routineTypeName() != null ? context.routineTypeName() : "없음").append('\n');
            sb.append("[연속 지속일] ").append(context.streakDays() != null ? context.streakDays() : 0).append("일\n");
        }

        sb.append("[오늘 완료한 스텝]\n");
        for (CompletedStep step : context.completedSteps()) {
            sb.append("- %s (사용 성분: %s)\n".formatted(step.stepName(), String.join(", ", step.ingredientNames())));
        }
        return sb.toString();
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "routine_completion");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", buildJsonSchema());
        responseFormat.set("json_schema", jsonSchema);

        return responseFormat;
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("completion_summary_text", objectMapper.createObjectNode().put("type", "string"));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.set("properties", rootProperties);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("completion_summary_text");
        root.set("required", required);
        root.put("additionalProperties", false);
        return root;
    }

    private LlmRoutineCompletionResult parseResult(JsonNode response) {
        try {
            String content = response.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);
            return new LlmRoutineCompletionResult(parsed.get("completion_summary_text").asText());
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패: {}", response, e);
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }
    }

    private String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/routine-completion-system.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
