package com.likelion.duckswell.domain.diagnosis.client.llm;

import java.util.List;

public record LlmDiagnosisResult(String summaryText, List<LlmDifficultyOption> difficultyOptions) {
}
