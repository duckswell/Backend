package com.likelion.duckswell.domain.diagnosis.client.llm;

import com.likelion.duckswell.domain.diagnosis.client.cv.CvScoreResult;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.List;

/** LLM 호출 (분석요약+강도옵션 생성)에 필요한 컨텍스트를 모은 값. */
public record LlmDiagnosisContext(
        String imagePath,
        List<Symptom> symptoms,
        String symptomNote,
        CvScoreResult todayScores,
        YesterdayContext yesterday,
        List<ProcedureResponse> procedures
) {
    public record YesterdayContext(
            List<Symptom> symptoms,
            String symptomNote,
            CvScoreResult scores,
            String summaryText
    ) {
    }
}
