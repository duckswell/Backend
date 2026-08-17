package com.likelion.duckswell.domain.diagnosis.dto;

import com.likelion.duckswell.domain.routine.entity.Symptom;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DiagnosisSubmitRequest(
        @NotNull Long courseId,
        @NotEmpty(message = "증상은 최소 1개 이상 선택해야 합니다. 특별한 증상이 없으면 NONE을 선택하세요.")
        List<Symptom> symptoms,
        String symptomNote,
        String photoId
) {
}
