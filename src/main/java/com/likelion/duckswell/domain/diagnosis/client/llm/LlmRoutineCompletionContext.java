package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.likelion.duckswell.domain.course.entity.CourseType;
import java.util.List;

public record LlmRoutineCompletionContext(
        CourseType courseType,
        String routineTypeName,
        Integer streakDays,
        List<CompletedStep> completedSteps
) {
    public record CompletedStep(String stepName, List<String> ingredientNames) {
    }
}
