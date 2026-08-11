package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import java.util.List;

public record RoutineStepsResponse(
        Long routineId,
        RoutineDifficulty difficulty,
        String title,
        Integer estimatedMinutes,
        String reasonText,
        List<RoutineStepResponse> steps
) {
    public static RoutineStepsResponse from(Routine routine) {
        return new RoutineStepsResponse(
                routine.getId(),
                routine.getDifficulty(),
                routine.getDifficulty().getTitle(),
                routine.getEstimatedMinutes(),
                routine.getReasonText(),
                routine.getSteps().stream().map(RoutineStepResponse::from).toList()
        );
    }
}
