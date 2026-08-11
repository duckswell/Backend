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
                    현재 날씨(자외선, 습도, 미세먼지)를 기준으로 오늘의 피부 케어 안내 문구를 반환합니다.
                    lat/lon을 생략하면(예: 위치 권한 거부) 서울 좌표를 기본값으로 사용합니다.
                    데일리 코스가 진행 중일 때만 배너를 노출하며, 집중 코스이거나 진행 중인 코스가 없으면 data 없이 응답합니다.
                    """
    )
    ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(Double lat, Double lon);
}
