package com.likelion.duckswell.domain.course.dto;

import java.util.List;

/** 공개 API 응답이 아니라 내부 조회용 - 루틴 스텝 생성 LLM 컨텍스트, 추천 제품 picker에서만 쓰인다. */
public record IngredientCandidateResponse(
        Long ingredientId,
        String name,
        List<String> tags
) {
}
