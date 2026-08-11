package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.Routine;
import java.time.LocalDateTime;
import java.util.List;

public record RoutineCompleteResponse(
        String completionSummaryText,
        List<String> recommendedIngredients,
        LocalDateTime completedAt
) {
    public static RoutineCompleteResponse of(Routine routine, List<String> recommendedIngredients) {
        return new RoutineCompleteResponse(routine.getCompletionSummaryText(), recommendedIngredients, routine.getCompletedAt());
    }
}
