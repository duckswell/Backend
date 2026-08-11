package com.likelion.duckswell.domain.weather.dto;

public record WeatherResponse(
        double tempC,
        String conditionText,
        int humidity,
        double uvIndex,
        double pm10,
        double pm2_5,
        int usEpaIndex
) {
}
