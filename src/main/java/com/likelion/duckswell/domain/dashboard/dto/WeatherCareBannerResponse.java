package com.likelion.duckswell.domain.dashboard.dto;

public record WeatherCareBannerResponse(
        WeatherIndicatorCard uv,
        WeatherIndicatorCard humidity,
        WeatherIndicatorCard dust,
        String summaryMessage,
        String triggerFactor
) {
}
