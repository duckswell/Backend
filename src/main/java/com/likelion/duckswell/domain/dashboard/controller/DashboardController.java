package com.likelion.duckswell.domain.dashboard.controller;

import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.domain.dashboard.service.ChecklistService;
import com.likelion.duckswell.domain.dashboard.service.WeatherCareBannerService;
import com.likelion.duckswell.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final WeatherCareBannerService weatherCareBannerService;
    private final ChecklistService checklistService;

    @Override
    @GetMapping("/weather-banner")
    public ResponseEntity<ApiResponse<WeatherCareBannerResponse>> getWeatherCareBanner(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon
    ) {
        return ResponseEntity.ok(ApiResponse.success(weatherCareBannerService.getBanner(lat, lon).orElse(null)));
    }

    @Override
    @GetMapping("/checklist")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> getTodayChecklist(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon
    ) {
        return ResponseEntity.ok(ApiResponse.success(checklistService.getTodayChecklist(lat, lon)));
    }

    @Override
    @PatchMapping("/checklist/{checklistItemId}/toggle")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> toggleChecklistItem(
            @PathVariable(name = "checklistItemId") Long checklistItemId
    ) {
        return ResponseEntity.ok(ApiResponse.success(checklistService.toggleCheck(checklistItemId)));
    }
}
