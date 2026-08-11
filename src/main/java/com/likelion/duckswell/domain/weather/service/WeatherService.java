package com.likelion.duckswell.domain.weather.service;

import com.likelion.duckswell.domain.weather.client.WeatherClient;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final double DEFAULT_LAT = 37.5665;
    private static final double DEFAULT_LON = 126.9780;

    private final WeatherClient weatherClient;

    public WeatherResponse getCurrentWeather(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return weatherClient.getCurrentWeather(DEFAULT_LAT, DEFAULT_LON);
        }
        return weatherClient.getCurrentWeather(lat, lon);
    }
}
