package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.List;

public record LlmRoutineStepsContext(
        RoutineDifficulty difficulty,
        List<Symptom> symptoms,
        String symptomNote,
        Integer rednessScore,
        Integer textureScore,
        Integer blemishScore,
        String diagnosisSummaryText,
        List<ProcedureResponse> procedures,
        List<IngredientCandidate> candidates
) {
    public record IngredientCandidate(Long ingredientId, String name, List<String> tags) {
    }
}
