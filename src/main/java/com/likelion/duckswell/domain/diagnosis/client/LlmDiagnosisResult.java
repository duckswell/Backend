package com.likelion.duckswell.domain.diagnosis.client;

import java.util.List;

public record LlmDiagnosisResult(String summaryText, List<LlmDifficultyOption> difficultyOptions) {
}
