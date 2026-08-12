package com.likelion.duckswell.domain.diagnosis.dto;

public record DiagnosisScoreSnapshot(
        Integer rednessScore,
        Integer textureScore,
        Integer blemishScore
) {
}
