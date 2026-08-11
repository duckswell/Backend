package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.List;

public record LlmRecoveryStageContext(
        int dayNumber,
        boolean hasYesterday,
        List<Symptom> yesterdaySymptoms,
        String yesterdaySymptomNote,
        String yesterdayCompletionSummaryText
) {
}
