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

    /** 자외선은 오늘 하루 중 최고치, 습도는 오늘 평균 - 생성 시점에 따라 값이 흔들리지 않도록 하루 단위로 조회한다. */
    public WeatherResponse getTodayForecast(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return weatherClient.getTodayForecast(DEFAULT_LAT, DEFAULT_LON);
        }
        return weatherClient.getTodayForecast(lat, lon);
    }
}
