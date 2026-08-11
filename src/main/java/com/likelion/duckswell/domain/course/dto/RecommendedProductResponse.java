package com.likelion.duckswell.domain.course.dto;

import com.likelion.duckswell.domain.product.dto.ProductResponse;

public record RecommendedProductResponse(
        String ingredientName,
        ProductResponse product
) {
}
