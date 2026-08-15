package com.likelion.duckswell.domain.dashboard.controller;

import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
import com.likelion.duckswell.domain.dashboard.dto.RecoveryBannerResponse;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Dashboard", description = "홈 화면 대시보드 API")
public interface DashboardApi {

    @Operation(
            summary = "날씨 기반 피부 케어 배너 조회",
            description = """
                    현재 날씨(자외선, 습도, 미세먼지) 기준으로 지표별 카드와 오늘의 종합 피부 케어 문구를 반환합니다.

                    uv/humidity/dust: 각 지표의 실측값(value), 등급 구간(level), 카드 상태명(cardStatus),
                    사이렌 노출 여부(siren, boolean)
                    summaryMessage: 세 지표 중 가장 주의가 필요한 지표를 기준으로 결정된 종합 안내 문구.
                    줄바꿈 위치에 개행 문자(\\n)가 포함되어 있으므로 클라이언트에서 그대로 개행 처리하면 됩니다.
                    triggerFactor: summaryMessage가 어떤 지표/등급 때문에 나왔는지 나타내는 근거 (예: "자외선 매우높음·위험", "모두 양호")

                    siren은 지표별 카드 상태 기준으로 다음 구간부터 true입니다.
                    - 자외선: 높음(6) 이상
                    - 습도: 30% 미만이거나 70% 이상
                    - 미세먼지: 81 이상

                    세 지표를 각각 심각도(양호/주의/심각)로 평가해 가장 심각도가 높은 지표 하나를 골라 종합 문구를 결정합니다.
                    심각도가 동일하게 겹치는 경우에만 자외선 > 습도 > 미세먼지 순으로 우선합니다.
                    lat/lon을 생략하면(예: 위치 권한 거부) 서울 좌표를 기본값으로 사용합니다.
                    집중 코스가 진행 중일 때만 배너를 숨기며(집중 코스는 별도 회복 배너가 대신 노출됨),
                    데일리 코스이거나 진행 중인 코스가 아예 없으면(신규 유저) 배너를 노출합니다.
                    진행 중인 코스가 없을 때는 지표 카드는 그대로 실제 날씨값을 반환하되, summaryMessage는
                    "시술 정보를 등록하고\\n집중 코스를 시작해보세요"로 고정됩니다.
                    """
    )
    ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon
    );

    @Operation(
            summary = "집중 코스 회복 배너 조회",
            description = """
                    집중 코스가 진행 중일 때만 노출되는 홈 화면 배너입니다. 진행 중인 코스가
                    없거나 데일리 코스면 data 없이 응답합니다(그 경우엔 날씨 배너가 대신 노출됩니다).

                    redness/texture/blemish: 오늘·어제 진단 점수(current/previous)와 그 차이(delta,
                    +는 악화·-는 개선). 어제 기록이 없으면(집중 코스 첫날 등) previous/delta는 0으로
                    고정되고, 오늘 진단 기록이 없으면 current도 0입니다.
                    summaryMessage: 가장 최근 등록된 시술일(없으면 코스 시작일) 기준 경과일수로 결정되는
                    회복 단계 안내 문구입니다 - 점수와 무관하게 날짜로만 결정됩니다.
                    줄바꿈 위치에 개행 문자(\\n)가 포함되어 있으므로 클라이언트에서 그대로 개행 처리하면 됩니다.
                    """
    )
    ResponseEntity<ApiResponse<RecoveryBannerResponse>> getRecoveryBanner();

    @Operation(
            summary = "오늘의 AI 체크리스트 조회",
            description = """
                    진행 중인 코스를 기준으로 오늘의 AI 체크리스트 2개를 반환합니다. 오늘 이미
                    생성된 항목이 있으면 그대로 재사용하고, 없으면 새로 생성합니다.

                    집중 코스: 등록된 시술 내역과 최근 루틴 기록을 근거로 시술 후 주의사항 체크리스트를 생성합니다.
                    데일리 코스: 오늘 하루 예보(자외선 최고치, 평균 습도, 미세먼지 등)와 최근 데일리 루틴 기록을 근거로
                    날씨 기반 케어 체크리스트를 생성합니다. 배너와 달리 생성 시점의 실시간 날씨가 아니라 하루 전체
                    요약을 사용해, 아침에 생성돼도 저녁 상황과 크게 어긋나지 않도록 합니다.
                    lat/lon을 생략하면(예: 위치 권한 거부) 서울 좌표를 기본값으로 사용합니다.
                    진행 중인 코스가 없으면 빈 목록을 반환합니다.
                    """
    )
    ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> getTodayChecklist(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon
    );

    @Operation(
            summary = "체크리스트 항목 체크 상태 토글",
            description = "체크리스트 항목 하나의 체크 여부를 반대로 뒤집습니다(체크↔체크 취소)."
    )
    ResponseEntity<ApiResponse<ChecklistItemResponse>> toggleChecklistItem(
            @PathVariable(name = "checklistItemId") Long checklistItemId
    );
}
