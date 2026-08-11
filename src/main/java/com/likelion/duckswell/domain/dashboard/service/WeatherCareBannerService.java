package com.likelion.duckswell.domain.dashboard.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherCareBannerService {

    private static final double UV_VERY_HIGH_THRESHOLD = 8;
    private static final double UV_HIGH_THRESHOLD = 6;
    private static final int HUMIDITY_VERY_LOW_THRESHOLD = 30;
    private static final int HUMIDITY_VERY_HIGH_THRESHOLD = 80;
    private static final double DUST_VERY_BAD_THRESHOLD = 150;
    private static final double DUST_BAD_THRESHOLD = 80;

    private final WeatherService weatherService;
    private final CourseService courseService;

    public Optional<WeatherCareBannerResponse> getBanner(Double lat, Double lon) {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty() || currentCourse.get().courseType() != CourseType.DAILY) {
            return Optional.empty();
        }

        WeatherResponse weather = weatherService.getCurrentWeather(lat, lon);
        return Optional.of(new WeatherCareBannerResponse(resolveMessage(weather)));
    }

    private String resolveMessage(WeatherResponse weather) {
        if (weather.uvIndex() >= UV_VERY_HIGH_THRESHOLD) {
            return "오늘은 한낮의 야외 활동을 줄여 주세요";
        }
        if (weather.uvIndex() >= UV_HIGH_THRESHOLD) {
            return "오늘은 햇빛으로부터 피부를 보호해 주세요";
        }
        if (weather.humidity() < HUMIDITY_VERY_LOW_THRESHOLD) {
            return "오늘은 피부가 쉽게 건조해질 수 있어요";
        }
        if (weather.humidity() >= HUMIDITY_VERY_HIGH_THRESHOLD) {
            return "오늘은 땀이 피부에 오래 남지 않게 해주세요";
        }
        if (weather.pm10() > DUST_VERY_BAD_THRESHOLD) {
            return "오늘은 야외 활동을 가능한 줄여 주세요";
        }
        if (weather.pm10() > DUST_BAD_THRESHOLD) {
            return "오늘은 외출 후 피부를 깨끗이 씻어 주세요";
        }
        return "오늘은 기본 케어를 편안하게 이어가세요";
    }
}
