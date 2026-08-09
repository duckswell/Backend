package com.likelion.duckswell.domain.product.controller;

import com.likelion.duckswell.domain.product.dto.IngredientResponse;
import com.likelion.duckswell.domain.product.service.ProductRecommendationService;
import com.likelion.duckswell.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductRecommendationService productRecommendationService;

    @Override
    @GetMapping("/recommendations/ingredients")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getRecommendedIngredients() {
        return ResponseEntity.ok(ApiResponse.success(productRecommendationService.getRecommendedIngredients()));
    }
}
