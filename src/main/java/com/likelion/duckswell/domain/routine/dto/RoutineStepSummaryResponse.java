package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.product.entity.ProductCategory;

/** 스텝의 대표(첫번째) 성분 id/이름 + 제품 종류만 담은 요약. 클렌징 스텝처럼 성분이 없으면 ingredientId/ingredientName은 null. */
public record RoutineStepSummaryResponse(
        Long stepId,
        String stepName,
        ProductCategory category,
        Long ingredientId,
        String ingredientName
) {
}
