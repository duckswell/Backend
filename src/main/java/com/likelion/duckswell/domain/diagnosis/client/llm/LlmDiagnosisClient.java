package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.global.client.llm.OpenAiChatClient;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * LLM 호출 (사진+증상 → 분석요약 + 강도옵션 3개 생성) 클라이언트.
 * OpenAI Chat Completions API를 Structured Outputs(json_schema, strict)로 호출해서
 * 응답 형식을 스키마 레벨에서 강제한다 (파싱 실패를 원천적으로 줄이기 위함.)
 */
@Slf4j
@Component
public class LlmDiagnosisClient {

    private final OpenAiChatClient openAiChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String systemPrompt;

    public LlmDiagnosisClient(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.systemPrompt = loadSystemPrompt();
    }

    public LlmDiagnosisResult analyze(LlmDiagnosisContext context) {
        ObjectNode requestBody = buildRequestBody(context);
        JsonNode response = openAiChatClient.call(requestBody, DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        return parseResult(response);
    }

    private ObjectNode buildRequestBody(LlmDiagnosisContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", openAiChatClient.model());
        root.set("messages", buildMessages(context));
        root.set("response_format", buildResponseFormat());
        return root;
    }

    private ArrayNode buildMessages(LlmDiagnosisContext context) {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.set("content", buildUserContent(context));
        messages.add(userMessage);

        return messages;
    }

    private ArrayNode buildUserContent(LlmDiagnosisContext context) {
        ArrayNode content = objectMapper.createArrayNode();

        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", buildUserText(context));
        content.add(textPart);

        if (context.imagePath() != null) {
            ObjectNode imagePart = objectMapper.createObjectNode();
            imagePart.put("type", "image_url");
            ObjectNode imageUrl = objectMapper.createObjectNode();
            imageUrl.put("url", toDataUri(context.imagePath()));
            imagePart.set("image_url", imageUrl);
            content.add(imagePart);
        }

        return content;
    }

    private String buildUserText(LlmDiagnosisContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[오늘 - 사용자 자기보고, 확인/되풀이 금지] 선택 증상: ")
                .append(formatSymptoms(context.symptoms())).append('\n');
        if (context.symptomNote() != null && !context.symptomNote().isBlank()) {
            sb.append("[오늘 - 사용자 자기보고, 확인/되풀이 금지] 자유 서술: ").append(context.symptomNote()).append('\n');
        }
        if (context.todayScores() != null) {
            sb.append("[오늘 - 분석 데이터, 숫자 그대로 언급 금지, 질적으로 풀어서 서술] ")
                    .append("붉은기 %.1f%%, 요철 %.1f%%, 잡티 %.1f%%\n"
                            .formatted(context.todayScores().rednessPct(), context.todayScores().texturePct(), context.todayScores().blemishPct()));
        }

        LlmDiagnosisContext.YesterdayContext yesterday = context.yesterday();
        if (yesterday != null) {
            sb.append("[어제 - 사용자 자기보고, 확인/되풀이 금지] 선택 증상: ")
                    .append(formatSymptoms(yesterday.symptoms())).append('\n');
            if (yesterday.symptomNote() != null && !yesterday.symptomNote().isBlank()) {
                sb.append("[어제 - 사용자 자기보고, 확인/되풀이 금지] 자유 서술: ").append(yesterday.symptomNote()).append('\n');
            }
            if (yesterday.scores() != null) {
                sb.append("[어제 - 분석 데이터, 오늘과 비교할 기준값] 붉은기 %.1f%%, 요철 %.1f%%, 잡티 %.1f%%\n"
                        .formatted(yesterday.scores().rednessPct(), yesterday.scores().texturePct(), yesterday.scores().blemishPct()));
            }
            if (yesterday.summaryText() != null) {
                sb.append("[어제의 분석요약 - 참고용] ").append(yesterday.summaryText()).append('\n');
            }
        } else {
            sb.append("[어제 기록] 없음 (첫 기록이거나 어제는 기록하지 않음)\n");
        }

        List<ProcedureResponse> procedures = context.procedures();
        if (procedures != null && !procedures.isEmpty()) {
            sb.append("[등록된 시술 정보]\n");
            for (ProcedureResponse procedure : procedures) {
                sb.append("- %s (%s, %d/%d회, 부위: %s)\n".formatted(
                        procedure.procedureType(),
                        procedure.procedureDate(),
                        procedure.currentCount(),
                        procedure.totalCount(),
                        procedure.areas()
                ));
            }
        } else {
            sb.append("[등록된 시술 정보] 없음\n");
        }

        return sb.toString();
    }

    private String formatSymptoms(List<Symptom> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return "없음";
        }
        StringJoiner joiner = new StringJoiner(", ");
        symptoms.forEach(symptom -> joiner.add(symptom.name()));
        return joiner.toString();
    }

    private String toDataUri(String imagePath) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(imagePath));
            String mimeType = Files.probeContentType(Path.of(imagePath));
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            return "data:%s;base64,%s".formatted(mimeType, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "diagnosis_analysis");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", buildJsonSchema());
        responseFormat.set("json_schema", jsonSchema);

        return responseFormat;
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode optionSchema = objectMapper.createObjectNode();
        optionSchema.put("type", "object");
        ObjectNode optionProperties = objectMapper.createObjectNode();
        optionProperties.set("difficulty", stringEnum("LIGHT", "BASIC", "INTENSIVE"));
        optionProperties.set("subtitle_focus", stringType());
        optionProperties.set("step_preview", stringType());
        optionProperties.set("estimated_minutes", objectMapper.createObjectNode().put("type", "integer"));
        optionSchema.set("properties", optionProperties);
        optionSchema.set("required", stringArray("difficulty", "subtitle_focus", "step_preview", "estimated_minutes"));
        optionSchema.put("additionalProperties", false);

        ObjectNode optionsArray = objectMapper.createObjectNode();
        optionsArray.put("type", "array");
        optionsArray.set("items", optionSchema);
        optionsArray.put("minItems", 3);
        optionsArray.put("maxItems", 3);

        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("summary_text", stringType());
        rootProperties.set("difficulty_options", optionsArray);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.set("properties", rootProperties);
        root.set("required", stringArray("summary_text", "difficulty_options"));
        root.put("additionalProperties", false);
        return root;
    }

    private ObjectNode stringType() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode stringEnum(String... values) {
        ObjectNode node = stringType();
        ArrayNode enumArray = node.putArray("enum");
        for (String value : values) {
            enumArray.add(value);
        }
        return node;
    }

    private ArrayNode stringArray(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private LlmDiagnosisResult parseResult(JsonNode response) {
        try {
            String content = response.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            List<LlmDifficultyOptionJson> parsedOptions = objectMapper.convertValue(
                    parsed.get("difficulty_options"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LlmDifficultyOptionJson.class)
            );
            List<LlmDifficultyOption> options = parsedOptions.stream().map(LlmDifficultyOptionJson::toResult).toList();

            return new LlmDiagnosisResult(parsed.get("summary_text").asText(), options);
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패: {}", response, e);
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }
    }

    private record LlmDifficultyOptionJson(
            String difficulty,
            String subtitle_focus,
            String step_preview,
            int estimated_minutes
    ) {
        LlmDifficultyOption toResult() {
            return new LlmDifficultyOption(difficulty, subtitle_focus, step_preview, estimated_minutes);
        }
    }

    private String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/diagnosis-analysis-system.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
