package com.likelion.duckswell.domain.dashboard.controller;

import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

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
    ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(Double lat, Double lon);
}
