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

    private static final String ONBOARDING_SUMMARY_MESSAGE = "시술 정보를 등록하고\n집중 코스를 시작해보세요";
    private static final String ONBOARDING_TRIGGER_FACTOR = "코스 시작 전";

    private final WeatherService weatherService;
    private final CourseService courseService;

    /**
     * 집중 코스 진행 중일 때는 회복 배너가 대신 노출되므로 날씨 배너는 숨긴다. 진행 중인 코스가
     * 아예 없는 신규 유저에게는(위치 기반이라 사용자 정보와 무관하므로) 날씨 배너를 그대로 보여주되,
     * 종합 문구만 온보딩 안내로 대체한다.
     */
    public Optional<WeatherCareBannerResponse> getBanner(Double lat, Double lon) {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isPresent() && currentCourse.get().courseType() == CourseType.FOCUS) {
            return Optional.empty();
        }

        WeatherResponse weather = weatherService.getCurrentWeather(lat, lon);
        WeatherIndicatorCard uv = resolveUvCard(weather.uvIndex());
        WeatherIndicatorCard humidity = resolveHumidityCard(weather.humidity());
        WeatherIndicatorCard dust = resolveDustCard(weather.pm10());

        if (currentCourse.isEmpty()) {
            return Optional.of(new WeatherCareBannerResponse(
                    uv, humidity, dust, ONBOARDING_SUMMARY_MESSAGE, ONBOARDING_TRIGGER_FACTOR
            ));
        }

        WeatherCareTriggerFactor triggerFactor = resolveTriggerFactor(weather);
        return Optional.of(new WeatherCareBannerResponse(
                uv, humidity, dust,
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
            return new WeatherIndicatorCard(uvIndex, "위험", "외출자제", true);
        }
        if (uvIndex >= UV_VERY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "매우높음", "외출주의", true);
        }
        if (uvIndex >= UV_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "높음", "노출 주의", true);
        }
        if (uvIndex >= UV_MODERATE_THRESHOLD) {
            return new WeatherIndicatorCard(uvIndex, "보통", "차단필요", false);
        }
        return new WeatherIndicatorCard(uvIndex, "낮음", "부담적음", false);
    }

    private WeatherIndicatorCard resolveHumidityCard(int humidity) {
        if (humidity < HUMIDITY_VERY_LOW_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "30%미만", "매우건조", true);
        }
        if (humidity < HUMIDITY_LOW_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "30~39%", "건조주의", false);
        }
        if (humidity < HUMIDITY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "40~69%", "적정", false);
        }
        if (humidity < HUMIDITY_VERY_HIGH_THRESHOLD) {
            return new WeatherIndicatorCard(humidity, "70~79%", "다소습함", true);
        }
        return new WeatherIndicatorCard(humidity, "80%이상", "매우습함", true);
    }

    private WeatherIndicatorCard resolveDustCard(double pm10) {
        if (pm10 > DUST_BAD_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "매우나쁨", "외출자제", true);
        }
        if (pm10 > DUST_MODERATE_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "나쁨", "자극주의", true);
        }
        if (pm10 > DUST_GOOD_THRESHOLD) {
            return new WeatherIndicatorCard(pm10, "보통", "공기보통", false);
        }
        return new WeatherIndicatorCard(pm10, "좋음", "공기쾌적", false);
    }
}
