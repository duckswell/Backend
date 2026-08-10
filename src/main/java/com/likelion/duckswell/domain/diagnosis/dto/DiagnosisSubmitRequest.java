package com.likelion.duckswell.domain.diagnosis.dto;

import com.likelion.duckswell.domain.routine.entity.Symptom;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DiagnosisSubmitRequest(
        @NotNull Long courseId,
        List<Symptom> symptoms,
        String symptomNote,
        String photoId
) {
}
