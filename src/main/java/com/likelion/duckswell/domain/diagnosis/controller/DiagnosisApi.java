package com.likelion.duckswell.domain.diagnosis.controller;

import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.duckswell.domain.diagnosis.dto.PhotoCheckResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Diagnosis", description = "오늘의 피부 사진/증상 제출 → AI 분석 리포트 API")
public interface DiagnosisApi {

    @Operation(
            summary = "사진 촬영 품질 체크",
            description = """
                    사진을 찍은 직후 호출합니다. 사진은 여기서 딱 한 번 업로드되고, 통과하면 photoId를 반환합니다
                    그대로 보관했다가 "오늘의 진단 제출" 요청에 실어 보내면 됩니다.
                    품질 불량(얼굴 미인식/너무 어두움/너무 밝음/조명 불균일 등)이면 200이 아니라
                    각 사유에 맞는 에러 응답(4xx)으로 내려가고, 업로드된 파일은 바로 삭제됩니다.
                    """
    )
    ResponseEntity<ApiResponse<PhotoCheckResponse>> checkPhoto(
            @Parameter(description = "품질을 확인할 사진") MultipartFile photo
    );

    @Operation(
            summary = "AI 피부 분석 - 오늘 확인한 피부 상태",
            description = """
                    증상 체크+자유 서술과, 앞서 품질 체크를 통과해 받은 photoId를 함께 제출하면
                    CV 모델로 붉은기/요철/잡티 %를 계산하고, LLM으로 분석요약과 강도옵션 3개
                    (가벼운/기본/꼼꼼한 관리)를 생성해서 반환합니다.

                    요청 필드:
                    - courseId (필수): 진단을 기록할 코스 id. 진행 중인 코스여야 합니다. 데일리든, 집중이든 시작해야 해당 API 사용 가능.
                    - symptoms (필수): 오늘 선택한 증상 다중선택.
                      REDNESS(붉은기) | HEAT(열감) | STINGING(따가움) | DRYNESS(건조함) |
                      FLAKING(각질) | OILINESS(번들거림) | ITCHINESS(가려움) | SWELLING(붓기)
                    - symptomNote (선택): 자유 서술 텍스트.
                    - photoId (조건부 필수): "사진 촬영 품질 체크" API에서 받은 값. courseType이 FOCUS(집중)면 필수, DAILY(데일리)면 생략 가능.

                    - photoId는 품질 체크를 이미 통과한 사진이므로 여기서 품질을 다시 확인하지 않습니다.
                    - 하루에 여러 번 제출할 수 있습니다 - 제출할 때마다 새 기록으로 남습니다(덮어쓰지 않음).
                    - 강도옵션 3개는 이 응답에만 담겨 반환되고 DB에 저장되지 않습니다 
                    - 사용자가 하나를 선택하는 순간부터는 별도 API(다음 이슈에서 구현)에서 처리합니다.
                    - 응답의 difficultyOptions[].difficulty 값: LIGHT(가벼운 관리) | BASIC(기본 관리) | INTENSIVE(꼼꼼한 관리)
                    - title은 난이도별 고정값. subtitle/stepPreview/estimatedMinutes는 매번 분석 결과에 맞게 AI가 생성합니다.
                    """
    )
    ResponseEntity<ApiResponse<DiagnosisResponse>> submitDiagnosis(@Valid DiagnosisSubmitRequest request);
}
