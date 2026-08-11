package com.likelion.duckswell.domain.weather.controller;

import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import com.likelion.duckswell.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController implements WeatherApi {

    private final WeatherService weatherService;

    @Override
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        return ResponseEntity.ok(ApiResponse.success(weatherService.getCurrentWeather(lat, lon)));
    }
}
