package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsContext.IngredientCandidate;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * product_text의 성분 조합은 LLM이 후보군 id만 고르고(JSON Schema enum 강제) 실제 이름은
 * 이 클래스가 후보군에서 되찾아 조립한다 - 자유 텍스트에 맡기면 후보군 밖 성분을 지어내서(예:
 * 카모마일) 자유 텍스트로 노출하지 않는다. alternate는 후보군 밖 실제 성분도 허용해야 해서
 * (사용자 요청) 이름 자체를 LLM이 직접 쓰게 하고, id 매핑 없이 그대로 문구에 끼워 넣는다.
 */
@Slf4j
@Component
public class LlmRoutineStepsClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String systemPrompt;

    public LlmRoutineStepsClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = loadSystemPrompt();
    }

    public LlmRoutineStepsResult generate(LlmRoutineStepsContext context) {
        if (apiKey.isBlank() || model.isBlank()) {
            log.error("openai.api-key / openai.model 설정이 없습니다. application-local.yml 또는 "
                    + "application-prod.yml에 채워주세요.");
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }
        if (context.candidates() == null || context.candidates().isEmpty()) {
            log.error("성분 후보군이 비어 있어 루틴 스텝 스키마를 만들 수 없습니다. routine_type_ingredient 시드를 확인하세요.");
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

        return parseResult(response, context.candidates());
    }

    private ObjectNode buildRequestBody(LlmRoutineStepsContext context) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.set("messages", buildMessages(context));
        root.set("response_format", buildResponseFormat(context));
        return root;
    }

    private ArrayNode buildMessages(LlmRoutineStepsContext context) {
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

    private String buildUserText(LlmRoutineStepsContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[선택한 난이도] ").append(context.difficulty().name()).append('\n');
        sb.append("[오늘 선택 증상] ").append(formatSymptoms(context.symptoms())).append('\n');
        if (context.symptomNote() != null && !context.symptomNote().isBlank()) {
            sb.append("[오늘 자유 서술] ").append(context.symptomNote()).append('\n');
        }
        if (context.rednessScore() != null || context.textureScore() != null || context.blemishScore() != null) {
            sb.append("[오늘 분석 데이터 - 숫자 그대로 언급 금지, 어느 항목이 두드러지는지만 참고] 붉은기 %s%%, 요철 %s%%, 잡티 %s%%\n"
                    .formatted(context.rednessScore(), context.textureScore(), context.blemishScore()));
        }
        if (context.diagnosisSummaryText() != null) {
            sb.append("[오늘의 분석요약 - 참고용] ").append(context.diagnosisSummaryText()).append('\n');
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

        sb.append("[사용 가능한 성분 후보군 - ingredient_id는 반드시 이 id 중에서만 고를 것]\n");
        for (IngredientCandidate candidate : context.candidates()) {
            sb.append("- id=%d, 이름=%s, 태그=%s\n".formatted(
                    candidate.ingredientId(), candidate.name(), String.join(", ", candidate.tags())));
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

    private ObjectNode buildResponseFormat(LlmRoutineStepsContext context) {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "routine_steps");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", buildJsonSchema(context));
        responseFormat.set("json_schema", jsonSchema);

        return responseFormat;
    }

    private ObjectNode buildJsonSchema(LlmRoutineStepsContext context) {
        ObjectNode cleansingStepSchema = buildCleansingStepSchema();
        ObjectNode activeStepSchema = buildStepSchema(context.candidates());

        int[] activeStepCountRange = activeStepCountRange(context.difficulty());
        ObjectNode activeStepsArray = objectMapper.createObjectNode();
        activeStepsArray.put("type", "array");
        activeStepsArray.set("items", activeStepSchema);
        activeStepsArray.put("minItems", activeStepCountRange[0]);
        activeStepsArray.put("maxItems", activeStepCountRange[1]);

        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("reason_text", stringType());
        rootProperties.set("estimated_minutes", objectMapper.createObjectNode().put("type", "integer"));
        rootProperties.set("cleansing_step", cleansingStepSchema);
        rootProperties.set("active_steps", activeStepsArray);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.set("properties", rootProperties);
        root.set("required", stringArray("reason_text", "estimated_minutes", "cleansing_step", "active_steps"));
        root.put("additionalProperties", false);
        return root;
    }

    /** 클렌저는 성분이 아니라 제형 특성(약산성 등)으로 설명하는 제품이라 성분 그라운딩 자체를 하지 않는다. */
    private ObjectNode buildCleansingStepSchema() {
        ObjectNode stepSchema = objectMapper.createObjectNode();
        stepSchema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("step_name", stringType());
        properties.set("category", productCategoryEnum());
        properties.set("product_type", stringType());
        properties.set("method_text", stringType());
        stepSchema.set("properties", properties);
        stepSchema.set("required", stringArray("step_name", "category", "product_type", "method_text"));
        stepSchema.put("additionalProperties", false);
        return stepSchema;
    }

    private ObjectNode buildStepSchema(List<IngredientCandidate> candidates) {
        ObjectNode ingredientIdsArray = objectMapper.createObjectNode();
        ingredientIdsArray.put("type", "array");
        ingredientIdsArray.set("items", integerEnum(candidates));
        ingredientIdsArray.put("minItems", 1);
        ingredientIdsArray.put("maxItems", 2);

        ObjectNode stepSchema = objectMapper.createObjectNode();
        stepSchema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("step_name", stringType());
        properties.set("category", productCategoryEnum());
        properties.set("product_type", stringType());
        properties.set("method_text", stringType());
        properties.set("ingredient_ids", ingredientIdsArray);
        properties.set("alternate_ingredient_name", stringType());
        stepSchema.set("required", stringArray("step_name", "category", "product_type", "method_text", "ingredient_ids", "alternate_ingredient_name"));
        stepSchema.set("properties", properties);
        stepSchema.put("additionalProperties", false);
        return stepSchema;
    }

    /** 상점 제품 조회용 고정 분류 - ProductCategory enum과 완전히 일치시킨다. */
    private ObjectNode productCategoryEnum() {
        ObjectNode node = objectMapper.createObjectNode().put("type", "string");
        ArrayNode enumArray = node.putArray("enum");
        for (ProductCategory category : ProductCategory.values()) {
            enumArray.add(category.name());
        }
        return node;
    }

    /** 첫 클렌징 스텝은 별도 필드로 고정하고, 여기 범위는 클렌징을 제외한 나머지 스텝 수. */
    private int[] activeStepCountRange(RoutineDifficulty difficulty) {
        return switch (difficulty) {
            case LIGHT -> new int[]{2, 3};
            case BASIC -> new int[]{3, 4};
            case INTENSIVE -> new int[]{4, 5};
        };
    }

    private ObjectNode stringType() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode integerEnum(List<IngredientCandidate> candidates) {
        ObjectNode node = objectMapper.createObjectNode().put("type", "integer");
        ArrayNode enumArray = node.putArray("enum");
        for (IngredientCandidate candidate : candidates) {
            enumArray.add(candidate.ingredientId());
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

    private LlmRoutineStepsResult parseResult(JsonNode response, List<IngredientCandidate> candidates) {
        try {
            String content = response.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            Map<Long, String> namesById = candidates.stream()
                    .collect(Collectors.toMap(IngredientCandidate::ingredientId, IngredientCandidate::name));

            CleansingStepJson cleansingStepJson = objectMapper.convertValue(parsed.get("cleansing_step"), CleansingStepJson.class);
            List<ActiveStepJson> activeStepJsons = objectMapper.convertValue(
                    parsed.get("active_steps"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ActiveStepJson.class)
            );

            List<LlmRoutineStepsResult.StepResult> steps = new ArrayList<>();
            steps.add(cleansingStepJson.toResult());
            activeStepJsons.forEach(step -> steps.add(step.toResult(namesById)));

            return new LlmRoutineStepsResult(
                    parsed.get("estimated_minutes").asInt(),
                    parsed.get("reason_text").asText(),
                    steps
            );
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패: {}", response, e);
            throw new CustomException(DiagnosisErrorCode.LLM_RESPONSE_INVALID);
        }
    }

    /**
     * 클렌저는 "OOO 성분의 클렌저" 식으로 성분명을 내세워 설명하지 않는다(약산성/저자극 등
     * 제형 특성으로 설명하는 게 자연스러움) - product_type을 그대로 노출 텍스트로 쓴다.
     * 특정 활성 성분과 묶이는 제품이 아니라서 ingredient_ids 자체를 grounding하지 않는다
     * (상점 제품 추천은 category만으로 조회).
     */
    private record CleansingStepJson(
            String step_name,
            String category,
            String product_type,
            String method_text
    ) {
        LlmRoutineStepsResult.StepResult toResult() {
            return new LlmRoutineStepsResult.StepResult(
                    step_name, ProductCategory.valueOf(category), product_type, method_text, null, List.of());
        }
    }

    private record ActiveStepJson(
            String step_name,
            String category,
            String product_type,
            String method_text,
            List<Long> ingredient_ids,
            String alternate_ingredient_name
    ) {
        LlmRoutineStepsResult.StepResult toResult(Map<Long, String> namesById) {
            List<Long> validIds = resolveValidIds(ingredient_ids, namesById);
            List<String> names = validIds.stream().map(namesById::get).toList();
            String productText = buildProductText(names, product_type);
            String alternateText = buildAlternateText(names, alternate_ingredient_name);
            return new LlmRoutineStepsResult.StepResult(
                    step_name, ProductCategory.valueOf(category), productText, method_text, alternateText, validIds);
        }

        private String buildAlternateText(List<String> primaryNames, String altName) {
            if (altName == null || altName.isBlank() || primaryNames.isEmpty()) {
                return null;
            }
            // alternate_ingredient_name은 자유 문자열이라 JSON 스키마로 ingredient_ids와의 중복을
            // 막을 수 없다 - LLM이 "센텔라가 없다면 센텔라를 사용해요"처럼 같은 성분을 대체
            // 성분으로 반환하면(프롬프트 위반) 문구 자체를 만들지 않고 건너뛴다.
            boolean altSameAsPrimary = primaryNames.stream().anyMatch(name -> name.equalsIgnoreCase(altName.trim()));
            if (altSameAsPrimary) {
                return null;
            }
            String joined = String.join("·", primaryNames);
            String lastPrimary = primaryNames.get(primaryNames.size() - 1);
            return "%s%s 없다면 %s%s 사용해요".formatted(
                    joined, particle(lastPrimary, "이", "가"), altName, particle(altName, "을", "를"));
        }

        private String particle(String word, String withBatchim, String withoutBatchim) {
            char last = word.charAt(word.length() - 1);
            if (last < 0xAC00 || last > 0xD7A3) {
                return withoutBatchim;
            }
            return (last - 0xAC00) % 28 != 0 ? withBatchim : withoutBatchim;
        }
    }

    private static List<Long> resolveValidIds(List<Long> ingredientIds, Map<Long, String> namesById) {
        List<Long> validIds = ingredientIds.stream().filter(namesById::containsKey).toList();
        if (validIds.isEmpty()) {
            throw new IllegalStateException("후보군에 없는 ingredient_id만 반환되었습니다: " + ingredientIds);
        }
        return validIds;
    }

    private static String buildProductText(List<String> names, String productType) {
        return "%s 성분의 %s".formatted(String.join("·", names), productType);
    }

    private String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/routine-steps-system.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
