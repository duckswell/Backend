package com.likelion.duckswell.domain.course.dto;

import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.List;

public record CourseSymptomSummaryResponse(
        List<SymptomFrequency> topSymptoms,
        RoutineTypeCode recommendedRoutineTypeCode,
        String recommendedRoutineTypeName
) {
    public record SymptomFrequency(
            Symptom symptom,
            long count
    ) {
    }
}
