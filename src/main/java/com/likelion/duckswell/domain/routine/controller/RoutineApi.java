package com.likelion.duckswell.domain.routine.controller;

import com.likelion.duckswell.domain.course.dto.RecommendedProductResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineCompleteResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineStepSummaryResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineStepsResponse;
import com.likelion.duckswell.domain.routine.dto.SelectDifficultyRequest;
import com.likelion.duckswell.domain.routine.dto.TodayRoutineResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Routine", description = "난이도 선택 → 루틴 스텝 생성/완료 API")
public interface RoutineApi {

    @Operation(
            summary = "지금 진행 중인 루틴 id 조회",
            description = """
                    현재 진행중인 코스는 항상 하나뿐이므로, 오늘 가장 최근 루틴의 id 하나만
                    반환합니다. 클라이언트가 routineId를 잃어버렸을 때(재접속 등) 다시 얻기
                    위한 용도입니다. 진행중인 코스가 없거나 오늘 루틴이 없으면 data가 null입니다.
                    """
    )
    ResponseEntity<ApiResponse<TodayRoutineResponse>> getTodayRoutine();

    @Operation(
            summary = "관리 난이도 선택 - 루틴 스텝 생성",
            description = """
                    진단 제출(POST /api/diagnoses) 응답의 difficultyOptions 중 하나를 고르면
                    호출합니다. 3개 옵션을 미리 다 만들어두지 않고 여기서 선택된 것만 실제 루틴
                    스텝으로 생성해서 저장하고 그대로 반환합니다.

                    요청 바디의 difficulty는 다음 3개 값 중 하나입니다:
                    - LIGHT(가벼운 관리): 3~4단계, 5분 안팎
                    - BASIC(기본 관리): 4~5단계, 10~20분
                    - INTENSIVE(꼼꼼한 관리): 5~6단계, 20~35분

                    예시: {"difficulty": "BASIC"}

                    응답의 각 스텝은 productText(제품+성분 조합)/methodText(사용법)/
                    alternateText(대체 성분 안내, 클렌징 제외 항상 채워짐)를 포함합니다.

                    category는 ProductCategory(CLEANSER/SKIN_TONER/AMPOULE_SERUM/CREAM/MIST_OIL)
                    값이며, 이 스텝에서 추천할 상점 제품을 카테고리로 좁혀 조회할 때 씁니다.

                    ingredientId는 sql/seed_shop_demo.sql 시드 순서 기준 고정값입니다:
                    1=비타민C, 2=나이아신아마이드, 3=히알루론산, 4=세라마이드, 5=판테놀,
                    6=센텔라, 7=알로에, 8=징크.

                    이미 난이도를 선택한 루틴에 다시 호출하면(재선택), 이전에 생성됐던 스텝은
                    전부 지워지고 새로 생성된 스텝으로 교체됩니다.
                    """
    )
    ResponseEntity<ApiResponse<RoutineStepsResponse>> selectDifficulty(Long routineId, @Valid SelectDifficultyRequest request);

    @Operation(
            summary = "루틴 완료 - 오늘의 루틴 기록 요약",
            description = """
                    생성된 스텝을 한 화면에 다 보여준 뒤, 사용자가 "루틴 완료" 버튼을 누르면
                    호출합니다(스텝별 체크 없음). 난이도가 아직 선택되지 않아 스텝이 없으면 400
                    으로 거부됩니다.

                    완료된 스텝과 사용 성분을 근거로 "오늘의 루틴 기록" 완료 요약을 생성해서
                    저장하고 반환합니다. 코스 타입(FOCUS/DAILY)에 따라 문체가 다릅니다:
                    - FOCUS: 오늘 완료한 스텝이 다룬 고민과 스텝 개수를 요약
                    - DAILY: 스텝 개수 언급 없이 "오늘도 ~을 관리하는 루틴을 실천했어요" 톤 +
                      꾸준함 격려. 같은 관리 타입을 7일 이상 연속으로 이어왔으면, 다른 관리
                      타입으로 바꿔보는 것도 고려해보라는 뉘앙스가 격려 문장 대신 들어갑니다.

                    recommendedIngredients는 AI가 만드는 게 아니라, 이 루틴의 스텝에 실제로
                    쓰인 성분을 DB에서 그대로 모아 반환한 값입니다(예: ["센텔라", "판테놀"]).
                    """
    )
    ResponseEntity<ApiResponse<RoutineCompleteResponse>> completeRoutine(Long routineId);

    @Operation(
            summary = "스텝별 대표 성분 + 제품 종류 요약",
            description = """
                    각 스텝의 대표(첫번째) 성분 id/이름과 제품 종류(category)만 간단히 반환합니다.
                    
                    이 category+ingredientId로 상점 페이지를 해당 성분이 체크된 상태로 이동시키는 데 쓰시면 됩니다. 
                    클렌징 스텝처럼 성분이 없는 스텝은 ingredientId/ingredientName이 둘 다 null입니다.
                    """
    )
    ResponseEntity<ApiResponse<List<RoutineStepSummaryResponse>>> getStepSummaries(
            @Parameter(description = "루틴 id") Long routineId);

    @Operation(
            summary = "루틴 완료 기준 추천 제품 조회",
            description = """
                    이 루틴에 쓰인 성분(클렌저 제외)마다 해당 스텝 카테고리로 상점 제품을 1개씩 뽑아 리스트로 반환합니다
                    (개수 상한 없음, 성분당 제품이 없으면 결과에서  빠집니다).
                    
                    "루틴 완료" 버튼을 누른 뒤 이 API를 별도로 호출해서 오늘의 추천 제품을 보여주는 용도입니다.
                    """
    )
    ResponseEntity<ApiResponse<List<RecommendedProductResponse>>> getRecommendedProducts(
            @Parameter(description = "루틴 id") Long routineId);
}
