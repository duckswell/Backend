package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.RoutineStep;
import java.util.List;

public record RoutineStepResponse(
        Long stepId,
        Integer order,
        String stepName,
        String productText,
        String methodText,
        String alternateText,
        List<RoutineStepIngredientResponse> ingredients
) {
    public static RoutineStepResponse from(RoutineStep step) {
        return new RoutineStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getStepName(),
                step.getProductText(),
                step.getMethodText(),
                step.getAlternateText(),
                step.getIngredients().stream().map(RoutineStepIngredientResponse::from).toList()
        );
    }
}
