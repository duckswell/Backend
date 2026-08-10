package com.likelion.duckswell.domain.diagnosis.dto;

import com.likelion.duckswell.domain.diagnosis.client.LlmDifficultyOption;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;

public record DifficultyOptionResponse(
        RoutineDifficulty difficulty,
        String title,
        String subtitle,
        String stepPreview,
        Integer estimatedMinutes
) {
    // AI가 만든 subtitleFocus(내용, 매번 달라짐) 뒤에 난이도별 고정 어미를 코드가 항상 붙인다.
    // "케어를/만/까지"를 focus와 어미 사이에 끼워서, focus가 어떻게 끝나든(받침 유무 등)
    // 조사 호응 문제 없이 항상 자연스러운 문장이 되게 한다.
    private static final String LIGHT_TEMPLATE = "%s 케어만 빠르게";
    private static final String BASIC_TEMPLATE = "%s 케어를 균형 있게";
    private static final String INTENSIVE_TEMPLATE = "%s 케어까지 빠짐없이";

    public static DifficultyOptionResponse from(LlmDifficultyOption option) {
        RoutineDifficulty difficulty = RoutineDifficulty.valueOf(option.difficulty());
        return new DifficultyOptionResponse(
                difficulty,
                difficulty.getTitle(),
                buildSubtitle(difficulty, option.subtitleFocus()),
                option.stepPreview(),
                option.estimatedMinutes()
        );
    }

    private static String buildSubtitle(RoutineDifficulty difficulty, String focus) {
        String template = switch (difficulty) {
            case LIGHT -> LIGHT_TEMPLATE;
            case BASIC -> BASIC_TEMPLATE;
            case INTENSIVE -> INTENSIVE_TEMPLATE;
        };
        return template.formatted(focus == null || focus.isBlank() ? "필요한" : focus.trim());
    }
}
