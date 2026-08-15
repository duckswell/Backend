package com.likelion.duckswell.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.dto.WeatherCareBannerResponse;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherCareBannerServiceTest {

    @Mock
    private WeatherService weatherService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private WeatherCareBannerService weatherCareBannerService;

    @Test
    void 진행중인_코스가_없으면_지표는_실제_날씨값으로_문구는_온보딩_안내로_노출한다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.empty());
        when(weatherService.getCurrentWeather(any(), any())).thenReturn(weather(1.6, 34, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.uv().value()).isEqualTo(1.6);
        assertThat(banner.summaryMessage()).isEqualTo("시술 정보를 등록하고\n집중 코스를 시작해보세요");
        assertThat(banner.triggerFactor()).isEqualTo("코스 시작 전");
    }

    @Test
    void 집중_코스가_진행중이면_배너를_노출하지_않는다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.of(currentCourse(CourseType.FOCUS)));

        // when
        Optional<WeatherCareBannerResponse> banner = weatherCareBannerService.getBanner(null, null);

        // then
        assertThat(banner).isEmpty();
    }

    @Test
    void 데일리_코스면_지표별_카드가_모두_함께_내려간다() {
        // given
        givenDailyCourseWithWeather(weather(1.6, 34, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.uv()).satisfies(card -> {
            assertThat(card.value()).isEqualTo(1.6);
            assertThat(card.level()).isEqualTo("낮음");
            assertThat(card.cardStatus()).isEqualTo("부담적음");
            assertThat(card.siren()).isFalse();
        });
        assertThat(banner.humidity()).satisfies(card -> {
            assertThat(card.value()).isEqualTo(34);
            assertThat(card.level()).isEqualTo("30~39%");
            assertThat(card.cardStatus()).isEqualTo("건조주의");
            assertThat(card.siren()).isFalse();
        });
        assertThat(banner.dust()).satisfies(card -> {
            assertThat(card.value()).isEqualTo(14.5);
            assertThat(card.level()).isEqualTo("좋음");
            assertThat(card.cardStatus()).isEqualTo("공기쾌적");
            assertThat(card.siren()).isFalse();
        });
    }

    @Test
    void 자외선이_높음이면_노출_주의_카드_상태와_사이렌이_함께_내려간다() {
        // given
        givenDailyCourseWithWeather(weather(6.0, 50, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.uv()).satisfies(card -> {
            assertThat(card.value()).isEqualTo(6.0);
            assertThat(card.level()).isEqualTo("높음");
            assertThat(card.cardStatus()).isEqualTo("노출 주의");
            assertThat(card.siren()).isTrue();
        });
    }

    @Test
    void 자외선이_보통이면_사이렌이_울리지_않는다() {
        // given
        givenDailyCourseWithWeather(weather(5.0, 50, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.uv().siren()).isFalse();
    }

    @Test
    void 습도가_다소습함이면_사이렌이_울린다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 70, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.humidity().siren()).isTrue();
    }

    @Test
    void 미세먼지가_나쁨이면_사이렌이_울린다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 50, 81));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.dust().siren()).isTrue();
    }

    @Test
    void 미세먼지가_보통이면_사이렌이_울리지_않는다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 50, 80));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.dust().siren()).isFalse();
    }

    @Test
    void 습도_매우낮음이_심각도가_더_높아서_자외선_높음보다_우선한다() {
        // given
        givenDailyCourseWithWeather(weather(6.0, 20, 10));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n피부가 쉽게 건조해질 수 있어요");
        assertThat(banner.triggerFactor()).isEqualTo("습도 매우낮음");
    }

    @Test
    void 미세먼지_매우나쁨이_심각도가_더_높아서_자외선_높음보다_우선한다() {
        // given
        givenDailyCourseWithWeather(weather(6.0, 50, 200));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n야외 활동을 가능한 줄여 주세요");
        assertThat(banner.triggerFactor()).isEqualTo("미세먼지 매우나쁨");
    }

    @Test
    void 심각도가_동일하면_자외선_습도_미세먼지_순으로_우선한다() {
        // given
        givenDailyCourseWithWeather(weather(8.0, 20, 200));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n한낮의 야외 활동을 줄여 주세요");
        assertThat(banner.triggerFactor()).isEqualTo("자외선 매우높음·위험");
    }

    @Test
    void 습도가_매우낮으면_건조_문구가_노출된다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 29, 10));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n피부가 쉽게 건조해질 수 있어요");
        assertThat(banner.triggerFactor()).isEqualTo("습도 매우낮음");
    }

    @Test
    void 습도가_매우높으면_땀_문구가_노출된다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 80, 10));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n땀이 피부에 오래 남지 않게 해주세요");
        assertThat(banner.triggerFactor()).isEqualTo("습도 매우높음");
    }

    @Test
    void 미세먼지가_매우나쁨이면_야외활동_자제_문구가_노출된다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 50, 151));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n야외 활동을 가능한 줄여 주세요");
        assertThat(banner.triggerFactor()).isEqualTo("미세먼지 매우나쁨");
    }

    @Test
    void 미세먼지가_나쁨이면_세안_문구가_노출된다() {
        // given
        givenDailyCourseWithWeather(weather(1.0, 50, 100));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n외출 후 피부를 깨끗이 씻어 주세요");
        assertThat(banner.triggerFactor()).isEqualTo("미세먼지 나쁨");
    }

    @Test
    void 모든_지표가_양호하면_기본_케어_문구가_노출된다() {
        // given
        givenDailyCourseWithWeather(weather(1.6, 34, 14.5));

        // when
        WeatherCareBannerResponse banner = weatherCareBannerService.getBanner(null, null).orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("오늘은\n기본 케어를 편안하게 이어가세요");
        assertThat(banner.triggerFactor()).isEqualTo("모두 양호");
    }

    private void givenDailyCourseWithWeather(WeatherResponse weatherResponse) {
        when(courseService.getCurrentCourse()).thenReturn(Optional.of(currentCourse(CourseType.DAILY)));
        when(weatherService.getCurrentWeather(any(), any())).thenReturn(weatherResponse);
    }

    private CurrentCourseResponse currentCourse(CourseType courseType) {
        return new CurrentCourseResponse(1L, courseType, "테스트 코스", LocalDate.now(), 0);
    }

    private WeatherResponse weather(double uvIndex, int humidity, double pm10) {
        return new WeatherResponse(20.0, "Sunny", humidity, uvIndex, pm10, pm10, 1);
    }
}
