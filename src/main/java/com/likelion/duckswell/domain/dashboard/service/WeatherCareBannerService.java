package com.likelion.duckswell.domain.dashboard.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareTriggerFactor;
import com.likelion.duckswell.domain.dashboard.dto.WeatherIndicatorCard;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherCareBannerService {

    private static final double UV_MODERATE_THRESHOLD = 3;
    private static final double UV_HIGH_THRESHOLD = 6;
    private static final double UV_VERY_HIGH_THRESHOLD = 8;
    private static final double UV_EXTREME_THRESHOLD = 11;

    private static final int HUMIDITY_VERY_LOW_THRESHOLD = 30;
    private static final int HUMIDITY_LOW_THRESHOLD = 40;
    private static final int HUMIDITY_HIGH_THRESHOLD = 70;
    private static final int HUMIDITY_VERY_HIGH_THRESHOLD = 80;

    private static final double DUST_GOOD_THRESHOLD = 30;
    private static final double DUST_MODERATE_THRESHOLD = 80;
    private static final double DUST_BAD_THRESHOLD = 150;

    private final WeatherService weatherService;
    private final CourseService courseService;

    public Optional<WeatherCareBannerResponse> getBanner(Double lat, Double lon) {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty() || currentCourse.get().courseType() != CourseType.DAILY) {
            return Optional.empty();
        }

        WeatherResponse weather = weatherService.getCurrentWeather(lat, lon);
        WeatherCareTriggerFactor triggerFactor = resolveTriggerFactor(weather);

        return Optional.of(new WeatherCareBannerResponse(
                resolveUvCard(weather.uvIndex()),
                resolveHumidityCard(weather.humidity()),
                resolveDustCard(weather.pm10()),
                triggerFactor.getSummaryMessage(),
                triggerFactor.getDescription()
        ));
    }

    private WeatherCareTriggerFactor resolveTriggerFactor(WeatherResponse weather) {
        return Stream.of(
                        resolveUvFactor(weather.uvIndex()),
                        resolveHumidityFactor(weather.humidity()),
                        resolveDustFactor(weather.pm10())
                )
                .max(Comparator.comparingInt(WeatherCareTriggerFactor::getSeverity))
                .orElse(WeatherCareTriggerFactor.ALL_GOOD);
    }

    private WeatherCareTriggerFactor resolveUvFactor(double uvIndex) {
        if (uvIndex >= UV_VERY_HIGH_THRESHOLD) {
            return WeatherCareTriggerFactor.UV_VERY_HIGH_OR_EXTREME;
        }
        if (uvIndex >= UV_HIGH_THRESHOLD) {
            return WeatherCareTriggerFactor.UV_HIGH;
        }
        return WeatherCareTriggerFactor.ALL_GOOD;
    }

    private WeatherCareTriggerFactor resolveHumidityFactor(int humidity) {
        if (humidity < HUMIDITY_VERY_LOW_THRESHOLD) {
            return WeatherCareTriggerFactor.HUMIDITY_VERY_LOW;
        }
        if (humidity >= HUMIDITY_VERY_HIGH_THRESHOLD) {
            return WeatherCareTriggerFactor.HUMIDITY_VERY_HIGH;
        }
        return WeatherCareTriggerFactor.ALL_GOOD;
    }

    private WeatherCareTriggerFactor resolveDustFactor(double pm10) {
        if (pm10 > DUST_BAD_THRESHOLD) {
            return WeatherCareTriggerFactor.DUST_VERY_BAD;
        }
        if (pm10 > DUST_MODERATE_THRESHOLD) {
            return WeatherCareTriggerFactor.DUST_BAD;
        }
        return WeatherCareTriggerFactor.ALL_GOOD;
    }

    private WeatherIndicatorCard resolveUvCard(double uvIndex) {
        if (uvIndex >= UV_EXTREME_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "위험", "외출자제");
        }
        if (uvIndex >= UV_VERY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "매우높음", "외출주의");
        }
        if (uvIndex >= UV_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "높음", "노출주의");
        }
        if (uvIndex >= UV_MODERATE_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "보통", "차단필요");
        }
        return new WeatherIndicatorCard(uvIndex, "낮음", "부담적음");
    }

    private WeatherIndicatorCard resolveHumidityCard(int humidity) {
        if (humidity < HUMIDITY_VERY_LOW_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "30%미만", "매우건조");
        }
        if (humidity < HUMIDITY_LOW_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "30~39%", "건조주의");
        }
        if (humidity < HUMIDITY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "40~69%", "적정");
        }
        if (humidity < HUMIDITY_VERY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "70~79%", "다소습함");
        }
        return new WeatherIndicatorCard(humidity, "80%이상", "매우습함");
    }

    private WeatherIndicatorCard resolveDustCard(double pm10) {
        if (pm10 > DUST_BAD_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "매우나쁨", "외출자제");
        }
        if (pm10 > DUST_MODERATE_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "나쁨", "자극주의");
        }
        if (pm10 > DUST_GOOD_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "보통", "공기보통");
        }
        return new WeatherIndicatorCard(pm10, "좋음", "공기쾌적");
    }
}
