package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineSymptom;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.List;

/** diagnosis 등 다른 도메인에 넘겨줄 때 쓰는 Routine의 최소 정보 스냅샷 (엔티티 직접 노출 방지). */
public record RoutineSnapshot(
        Long id,
        Long courseId,
        String photoUrl,
        String symptomNote,
        List<Symptom> symptoms
) {
    public static RoutineSnapshot from(Routine routine) {
        return new RoutineSnapshot(
                routine.getId(),
                routine.getCourseId(),
                routine.getPhotoUrl(),
                routine.getSymptomNote(),
                routine.getSymptoms().stream().map(RoutineSymptom::getSymptom).toList()
        );
    }
}
