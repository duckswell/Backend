package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.likelion.duckswell.domain.product.entity.ProductCategory;
import java.util.List;

public record LlmRoutineStepsResult(
        Integer estimatedMinutes,
        String reasonText,
        List<StepResult> steps
) {
    /** alternateText는 ingredientIds가 비어있는 스텝(클렌징 등)에서는 null일 수 있다. */
    public record StepResult(
            String stepName,
            ProductCategory category,
            String productText,
            String methodText,
            String alternateText,
            List<Long> ingredientIds
    ) {
    }
}
