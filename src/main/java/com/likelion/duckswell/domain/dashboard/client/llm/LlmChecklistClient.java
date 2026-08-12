package com.likelion.duckswell.domain.dashboard.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.dashboard.exception.DashboardErrorCode;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult.ChecklistItemDraft;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * LLM 호출 (진행 코스 컨텍스트 → 오늘의 체크리스트 2개) 클라이언트.
 * FOCUS는 시술/루틴 이력을, DAILY는 오늘 날씨/루틴 이력을 근거로 삼는다 - 코스 타입별로
 * 시스템 프롬프트를 분리해 서로 다른 관점(시술 후 주의사항 vs 날씨 기반 케어)을 강제한다.
 */
@Slf4j
@Component
public class LlmChecklistClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int CHECKLIST_ITEM_COUNT = 2;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String focusSystemPrompt;
    private final String dailySystemPrompt;

    public LlmChecklistClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.focusSystemPrompt = loadSystemPrompt("prompts/checklist-focus-system.txt");
        this.dailySystemPrompt = loadSystemPrompt("prompts/checklist-daily-system.txt");
    }

    public LlmChecklistResult generate(LlmChecklistContext context) {
        if (apiKey.isBlank() || model.isBlank()) {
            log.error("openai.api-key / openai.model 설정이 없습니다. application-local.yml 또는 "
                    + "application-prod.yml에 채워주세요.");
            throw new CustomException(DashboardErrorCode.LLM_RESPONSE_INVALID);
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
                throw new CustomException(DashboardErrorCode.LLM_RESPONSE_INVALID);
            }
        } catch (IOException | InterruptedException e) {
            log.error("OpenAI 호출 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(DashboardErrorCode.LLM_RESPONSE_INVALID);
        }

        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (IOException e) {
            log.error("OpenAI 응답 JSON 파싱 실패: {}", responseBody, e);
            throw new CustomException(DashboardErrorCode.LLM_RESPONSE_INVALID);
        }

        return parseResult(response);
    }

    private ObjectNode buildRequestBody(LlmChecklistContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.set("messages", buildMessages(context));
        root.set("response_format", buildResponseFormat());
        return root;
    }

    private ArrayNode buildMessages(LlmChecklistContext context) {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPromptFor(context.courseType()));
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", buildUserText(context));
        messages.add(userMessage);

        return messages;
    }

    private String systemPromptFor(CourseType courseType) {
        return courseType == CourseType.FOCUS ? focusSystemPrompt : dailySystemPrompt;
    }

    private String buildUserText(LlmChecklistContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[오늘 날짜] ").append(context.today()).append('\n');
        if (context.courseType() == CourseType.FOCUS) {
            appendProcedures(sb, context.procedures());
        } else {
            sb.append("[진행 중인 데일리 루틴] ").append(context.routineTypeLabel()).append('\n');
            appendWeather(sb, context.weather());
        }
        appendRecentRoutines(sb, context.recentRoutines());

        return sb.toString();
    }

    private void appendProcedures(StringBuilder sb, List<ProcedureResponse> procedures) {
        if (procedures == null || procedures.isEmpty()) {
            sb.append("[등록된 시술 정보] 없음\n");
            return;
        }
        sb.append("[등록된 시술 정보]\n");
        for (ProcedureResponse procedure : procedures) {
            sb.append("- %s (%s, %d/%d회, 부위: %s)\n".formatted(
                    procedure.procedureTypeName(),
                    procedure.procedureDate(),
                    procedure.currentCount(),
                    procedure.totalCount(),
                    procedure.areas()
            ));
        }
    }

    private void appendWeather(StringBuilder sb, WeatherResponse weather) {
        sb.append("[오늘의 날씨] 기온 %.1f°C, %s, 습도 %d%%, 자외선지수 %.1f, 미세먼지(PM10) %.1f\n"
                .formatted(
                        weather.tempC(),
                        weather.conditionText(),
                        weather.humidity(),
                        weather.uvIndex(),
                        weather.pm10()
                ));
    }

    private void appendRecentRoutines(StringBuilder sb, List<RoutineSnapshot> recentRoutines) {
        if (recentRoutines == null || recentRoutines.isEmpty()) {
            sb.append("[최근 루틴 기록] 없음\n");
            return;
        }
        sb.append("[최근 루틴 기록]\n");
        for (RoutineSnapshot routine : recentRoutines) {
            sb.append("- %s: 선택 증상 %s%s\n".formatted(
                    routine.routineDate(),
                    formatSymptoms(routine.symptoms()),
                    routine.symptomNote() != null && !routine.symptomNote().isBlank()
                            ? " / 자유 서술: " + routine.symptomNote()
                            : ""
            ));
        }
    }

    private String formatSymptoms(List<Symptom> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return "없음";
        }
        return String.join(", ", symptoms.stream().map(Symptom::name).toList());
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "checklist_items");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", buildJsonSchema());
        responseFormat.set("json_schema", jsonSchema);

        return responseFormat;
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "object");
        ObjectNode itemProperties = objectMapper.createObjectNode();
        itemProperties.set("title", stringType());
        itemProperties.set("description", stringType());
        itemSchema.set("properties", itemProperties);
        itemSchema.set("required", stringArray("title", "description"));
        itemSchema.put("additionalProperties", false);

        ObjectNode itemsArray = objectMapper.createObjectNode();
        itemsArray.put("type", "array");
        itemsArray.set("items", itemSchema);
        itemsArray.put("minItems", CHECKLIST_ITEM_COUNT);
        itemsArray.put("maxItems", CHECKLIST_ITEM_COUNT);

        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("items", itemsArray);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.set("properties", rootProperties);
        root.set("required", stringArray("items"));
        root.put("additionalProperties", false);
        return root;
    }

    private ObjectNode stringType() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ArrayNode stringArray(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private LlmChecklistResult parseResult(JsonNode response) {
        try {
            String content = response.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            List<ChecklistItemDraftJson> parsedItems = objectMapper.convertValue(
                    parsed.get("items"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChecklistItemDraftJson.class)
            );
            List<ChecklistItemDraft> items = parsedItems.stream().map(ChecklistItemDraftJson::toDraft).toList();

            return new LlmChecklistResult(items);
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패: {}", response, e);
            throw new CustomException(DashboardErrorCode.LLM_RESPONSE_INVALID);
        }
    }

    private record ChecklistItemDraftJson(String title, String description) {
        ChecklistItemDraft toDraft() {
            return new ChecklistItemDraft(title, description);
        }
    }

    private String loadSystemPrompt(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
