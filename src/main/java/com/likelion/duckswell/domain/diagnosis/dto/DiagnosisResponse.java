package com.likelion.duckswell.domain.diagnosis.dto;

import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import java.util.List;

public record DiagnosisResponse(
        Long diagnosisId,
        Long routineId,
        Integer rednessScore,
        Integer textureScore,
        Integer blemishScore,
        String summaryText,
        List<DifficultyOptionResponse> difficultyOptions
) {
    public static DiagnosisResponse of(Diagnosis diagnosis, List<DifficultyOptionResponse> difficultyOptions) {
        return new DiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getRoutineId(),
                diagnosis.getRednessScore(),
                diagnosis.getTextureScore(),
                diagnosis.getBlemishScore(),
                diagnosis.getSummaryText(),
                difficultyOptions
        );
    }
}
