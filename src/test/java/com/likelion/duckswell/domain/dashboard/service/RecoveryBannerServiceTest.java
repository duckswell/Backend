package com.likelion.duckswell.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.dto.RecoveryBannerResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisScoreSnapshot;
import com.likelion.duckswell.domain.diagnosis.service.DiagnosisService;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecoveryBannerServiceTest {

    @Mock
    private CourseService courseService;

    @Mock
    private ProcedureService procedureService;

    @Mock
    private DiagnosisService diagnosisService;

    @InjectMocks
    private RecoveryBannerService recoveryBannerService;

    @Test
    void 진행중인_코스가_없으면_배너를_노출하지_않는다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.empty());

        // when
        Optional<RecoveryBannerResponse> banner = recoveryBannerService.getBanner();

        // then
        assertThat(banner).isEmpty();
    }

    @Test
    void 데일리_코스면_배너를_노출하지_않는다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.of(currentCourse(CourseType.DAILY, LocalDate.now())));

        // when
        Optional<RecoveryBannerResponse> banner = recoveryBannerService.getBanner();

        // then
        assertThat(banner).isEmpty();
    }

    @Test
    void 오늘_새_기록이_생기면_그_기록이_현재값_기존_현재값이_직전값이_된다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(3));
        when(diagnosisService.getRecentScores(anyInt())).thenReturn(List.of(
                new DiagnosisScoreSnapshot(20, 30, 10),
                new DiagnosisScoreSnapshot(25, 35, 15)
        ));

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.redness()).satisfies(card -> {
            assertThat(card.current()).isEqualTo(20);
            assertThat(card.previous()).isEqualTo(25);
            assertThat(card.delta()).isEqualTo(-5);
        });
        assertThat(banner.texture().delta()).isEqualTo(-5);
        assertThat(banner.blemish().delta()).isEqualTo(-5);
    }

    @Test
    void 유효한_기록이_1개뿐이면_이전값과_변화량은_0이다() {
        // given
        givenFocusCourse(LocalDate.now());
        when(diagnosisService.getRecentScores(anyInt()))
                .thenReturn(List.of(new DiagnosisScoreSnapshot(20, 30, 10)));

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.redness()).satisfies(card -> {
            assertThat(card.current()).isEqualTo(20);
            assertThat(card.previous()).isEqualTo(0);
            assertThat(card.delta()).isEqualTo(0);
        });
    }

    @Test
    void 새_기록이_없으면_기존_기록들끼리_그대로_비교된다() {
        // given - "오늘" 기록이 없어도 getRecentScores가 돌려주는 최근 두 기록(예: 어제/그제)을
        // 그대로 current/previous로 쓰는지 검증한다. 오늘 날짜에 억지로 0을 끼워넣지 않는다.
        givenFocusCourse(LocalDate.now().minusDays(3));
        when(diagnosisService.getRecentScores(anyInt())).thenReturn(List.of(
                new DiagnosisScoreSnapshot(30, 40, 20),
                new DiagnosisScoreSnapshot(25, 35, 15)
        ));

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.redness()).satisfies(card -> {
            assertThat(card.current()).isEqualTo(30);
            assertThat(card.previous()).isEqualTo(25);
            assertThat(card.delta()).isEqualTo(5);
        });
    }

    @Test
    void 유효한_기록이_하나도_없으면_모두_0이다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(3));
        when(diagnosisService.getRecentScores(anyInt())).thenReturn(List.of());

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.redness()).satisfies(card -> {
            assertThat(card.current()).isEqualTo(0);
            assertThat(card.previous()).isEqualTo(0);
            assertThat(card.delta()).isEqualTo(0);
        });
    }

    @Test
    void 시술_후_1에서_3일차는_초기_문구가_노출된다() {
        // given
        givenFocusCourse(LocalDate.now());
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 1일째,\n피부 진정에 집중할 때예요");
    }

    @Test
    void 시술_후_4에서_5일차는_중기_문구가_노출된다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(4));
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 5일째,\n피부 장벽을 채워줄 시기예요");
    }

    @Test
    void 시술_후_6일차_이후는_후기_문구가_노출된다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(9));
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 10일째,\n흔적 개선을 시작해도 좋은 시기예요");
    }

    @Test
    void 등록된_시술이_없으면_코스_시작일_기준으로_경과일수를_계산한다() {
        // given
        LocalDate startedAt = LocalDate.now().minusDays(4);
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.FOCUS, "집중코스", startedAt, 0)));
        when(procedureService.getProceduresForCourse(anyLong())).thenReturn(List.of());
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 5일째,\n피부 장벽을 채워줄 시기예요");
    }

    private void givenFocusCourse(LocalDate procedureDate) {
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(currentCourse(CourseType.FOCUS, LocalDate.now())));
        when(procedureService.getProceduresForCourse(anyLong())).thenReturn(List.of(
                new ProcedureResponse(1L, ProcedureType.IPL_LASER_TONING, "IPL/레이저토닝", procedureDate, 1, 5, List.of())
        ));
    }

    private void givenNoDiagnosisRecords() {
        when(diagnosisService.getRecentScores(anyInt())).thenReturn(List.of());
    }

    private CurrentCourseResponse currentCourse(CourseType courseType, LocalDate startedAt) {
        return new CurrentCourseResponse(1L, courseType, "테스트 코스", startedAt, 0);
    }
}
