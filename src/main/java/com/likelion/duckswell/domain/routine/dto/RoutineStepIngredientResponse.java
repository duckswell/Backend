package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.IngredientRole;
import com.likelion.duckswell.domain.routine.entity.RoutineStepIngredient;

public record RoutineStepIngredientResponse(Long ingredientId, IngredientRole role) {
    public static RoutineStepIngredientResponse from(RoutineStepIngredient ingredient) {
        return new RoutineStepIngredientResponse(ingredient.getIngredientId(), ingredient.getIngredientRole());
    }
}
