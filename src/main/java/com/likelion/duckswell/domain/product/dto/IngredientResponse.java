package com.likelion.duckswell.domain.product.dto;

import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientCategory;

public record IngredientResponse(
        Long id,
        String name,
        IngredientCategory category
) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getCategory());
    }
}
