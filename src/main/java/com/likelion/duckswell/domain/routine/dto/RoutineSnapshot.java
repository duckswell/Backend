package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineSymptom;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** diagnosis 등 다른 도메인에 넘겨줄 때 쓰는 Routine의 최소 정보 스냅샷 (엔티티 직접 노출 방지). */
public record RoutineSnapshot(
        Long id,
        Long courseId,
        LocalDate routineDate,
        String photoUrl,
        String symptomNote,
        List<Symptom> symptoms,
        LocalDateTime completedAt,
        String completionSummaryText,
        String recoveryStageSummaryText
) {
    public static RoutineSnapshot from(Routine routine) {
        return new RoutineSnapshot(
                routine.getId(),
                routine.getCourseId(),
                routine.getRoutineDate(),
                routine.getPhotoUrl(),
                routine.getSymptomNote(),
                routine.getSymptoms().stream().map(RoutineSymptom::getSymptom).toList(),
                routine.getCompletedAt(),
                routine.getCompletionSummaryText(),
                routine.getRecoveryStageSummaryText()
        );
    }
}
