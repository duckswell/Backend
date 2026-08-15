package com.likelion.duckswell.domain.dashboard.dto;

public record WeatherIndicatorCard(
        double value,
        String level,
        String cardStatus,
        boolean siren
) {
}
