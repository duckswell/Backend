package com.likelion.duckswell.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    void 오늘과_어제_기록이_모두_있으면_변화량을_계산한다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(3));
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now())))
                .thenReturn(Optional.of(new DiagnosisScoreSnapshot(20, 30, 10)));
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now().minusDays(1))))
                .thenReturn(Optional.of(new DiagnosisScoreSnapshot(25, 35, 15)));

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
    void 어제_기록이_없으면_이전값과_변화량은_0이다() {
        // given
        givenFocusCourse(LocalDate.now());
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now())))
                .thenReturn(Optional.of(new DiagnosisScoreSnapshot(20, 30, 10)));
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now().minusDays(1))))
                .thenReturn(Optional.empty());

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
    void 오늘_기록이_없으면_현재값은_0이다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(3));
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(diagnosisService.getScores(anyLong(), eq(LocalDate.now().minusDays(1))))
                .thenReturn(Optional.of(new DiagnosisScoreSnapshot(25, 35, 15)));

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.redness()).satisfies(card -> {
            assertThat(card.current()).isEqualTo(0);
            assertThat(card.previous()).isEqualTo(25);
            assertThat(card.delta()).isEqualTo(-25);
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
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 1일째, 피부 진정에 집중할 때예요");
    }

    @Test
    void 시술_후_4에서_5일차는_중기_문구가_노출된다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(4));
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 5일째, 피부 장벽을 채워줄 시기예요");
    }

    @Test
    void 시술_후_6일차_이후는_후기_문구가_노출된다() {
        // given
        givenFocusCourse(LocalDate.now().minusDays(9));
        givenNoDiagnosisRecords();

        // when
        RecoveryBannerResponse banner = recoveryBannerService.getBanner().orElseThrow();

        // then
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 10일째, 흔적 개선을 시작해도 좋은 시기예요");
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
        assertThat(banner.summaryMessage()).isEqualTo("시술 후 5일째, 피부 장벽을 채워줄 시기예요");
    }

    private void givenFocusCourse(LocalDate procedureDate) {
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(currentCourse(CourseType.FOCUS, LocalDate.now())));
        when(procedureService.getProceduresForCourse(anyLong())).thenReturn(List.of(
                new ProcedureResponse(1L, ProcedureType.IPL_LASER_TONING, "IPL/레이저토닝", procedureDate, 1, 5, List.of())
        ));
    }

    private void givenNoDiagnosisRecords() {
        when(diagnosisService.getScores(anyLong(), any())).thenReturn(Optional.empty());
    }

    private CurrentCourseResponse currentCourse(CourseType courseType, LocalDate startedAt) {
        return new CurrentCourseResponse(1L, courseType, "테스트 코스", startedAt, 0);
    }
}
