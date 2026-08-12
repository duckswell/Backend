package com.likelion.duckswell.domain.dashboard.controller;

import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
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

                    uv/humidity/dust: 각 지표의 실측값(value), 등급 구간(level), 카드 상태명(cardStatus)
                    summaryMessage: 세 지표 중 가장 주의가 필요한 지표를 기준으로 결정된 종합 안내 문구
                    triggerFactor: summaryMessage가 어떤 지표/등급 때문에 나왔는지 나타내는 근거 (예: "자외선 매우높음·위험", "모두 양호")

                    세 지표를 각각 심각도(양호/주의/심각)로 평가해 가장 심각도가 높은 지표 하나를 골라 종합 문구를 결정합니다.
                    심각도가 동일하게 겹치는 경우에만 자외선 > 습도 > 미세먼지 순으로 우선합니다.
                    lat/lon을 생략하면(예: 위치 권한 거부) 서울 좌표를 기본값으로 사용합니다.
                    데일리 코스가 진행 중일 때만 배너를 노출하며, 집중 코스이거나 진행 중인 코스가 없으면 data 없이 응답합니다.
                    """
    )
    ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon
    );

    @Operation(
            summary = "오늘의 AI 체크리스트 조회",
            description = """
                    진행 중인 코스를 기준으로 오늘의 AI 체크리스트 2개를 반환합니다. 오늘 이미
                    생성된 항목이 있으면 그대로 재사용하고, 없으면 새로 생성합니다.

                    집중 코스: 등록된 시술 내역과 최근 루틴 기록을 근거로 시술 후 주의사항 체크리스트를 생성합니다.
                    데일리 코스: 오늘 날씨와 최근 데일리 루틴 기록을 근거로 날씨 기반 케어 체크리스트를 생성합니다.
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
