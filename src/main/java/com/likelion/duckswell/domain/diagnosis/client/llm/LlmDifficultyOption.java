package com.likelion.duckswell.domain.diagnosis.client.llm;

/**
 * title은 고정.
 * subtitleFocus는 "무엇에 집중하는 케어인지"만 담은 짧은 구(AI 생성, 매번 달라짐) -
 * 어미("빠르게"/"균형 있게"/"빠짐없이")는 코드가 항상 고정으로 붙인다 (DifficultyOptionResponse 참고)
 */
public record LlmDifficultyOption(
        String difficulty,
        String subtitleFocus,
        String stepPreview,
        int estimatedMinutes
) {
}
