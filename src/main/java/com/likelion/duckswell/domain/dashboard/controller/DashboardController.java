package com.likelion.duckswell.domain.dashboard.controller;

import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.domain.dashboard.service.WeatherCareBannerService;
import com.likelion.duckswell.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final WeatherCareBannerService weatherCareBannerService;

    @Override
    @GetMapping("/weather-banner")
    public ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        return ResponseEntity.ok(ApiResponse.success(weatherCareBannerService.getBanner(lat, lon).orElse(null)));
    }
}
