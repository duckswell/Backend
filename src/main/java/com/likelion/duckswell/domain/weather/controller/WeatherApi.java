package com.likelion.duckswell.domain.weather.controller;

import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Weather", description = "날씨 정보 조회 API")
public interface WeatherApi {

    @Operation(
            summary = "현재 날씨 조회",
            description = """
                    위도/경도로 현재 날씨를 조회합니다. 기온, 날씨 상태, 습도, 자외선 지수,
                    미세먼지(PM10/PM2.5), 통합대기환경지수(US EPA Index)를 반환합니다.
                    lat/lon을 생략하면(예: 위치 권한 거부) 서울 좌표를 기본값으로 사용합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "날씨 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "날씨 정보를 가져오지 못함")
    })
    ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(Double lat, Double lon);
}
