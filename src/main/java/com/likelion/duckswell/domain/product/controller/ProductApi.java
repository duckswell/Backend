package com.likelion.duckswell.domain.product.controller;

import com.likelion.duckswell.domain.product.dto.IngredientResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Product", description = "제품 추천 상점 API")
public interface ProductApi {

    @Operation(
            summary = "맞춤 추천 성분 목록 조회",
            description = """
                    현재 진행중인 코스가 있으면 그 코스의 전체 루틴에서, 진행중인 코스가 없으면(공백기)
                    가장 최근에 완료한 코스의 최근 루틴들에서 추천된 성분 중, 가장 많이 등장한 순으로
                    최대 3개(계열 포함)를 반환합니다. "나의 맞춤 성분" 화면의 1단계 필터 탭에 사용됩니다.
                    """
    )
    ResponseEntity<ApiResponse<List<IngredientResponse>>> getRecommendedIngredients();
}
