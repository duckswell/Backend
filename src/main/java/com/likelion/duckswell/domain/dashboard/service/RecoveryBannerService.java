package com.likelion.duckswell.domain.dashboard.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.dto.RecoveryBannerResponse;
import com.likelion.duckswell.domain.dashboard.dto.RecoveryStage;
import com.likelion.duckswell.domain.dashboard.dto.SkinScoreCard;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisScoreSnapshot;
import com.likelion.duckswell.domain.diagnosis.service.DiagnosisService;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 집중 코스 진행 중일 때만 노출되는 홈 화면 회복 배너. 붉은기/요철/잡티는 오늘·어제 진단 점수의
 * 단순 차이(산술)라 AI를 쓰지 않고, 종합 문구도 시술 경과일수만으로 결정되는 소수의 경우의 수라
 * 날씨 배너와 동일하게 규칙 기반으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class RecoveryBannerService {

    private final CourseService courseService;
    private final ProcedureService procedureService;
    private final DiagnosisService diagnosisService;

    public Optional<RecoveryBannerResponse> getBanner() {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty() || currentCourse.get().courseType() != CourseType.FOCUS) {
            return Optional.empty();
        }
        CurrentCourseResponse course = currentCourse.get();

        LocalDate today = LocalDate.now();
        Optional<DiagnosisScoreSnapshot> todayScores = diagnosisService.getScores(course.courseId(), today);
        Optional<DiagnosisScoreSnapshot> yesterdayScores = diagnosisService.getScores(course.courseId(), today.minusDays(1));

        return Optional.of(new RecoveryBannerResponse(
                buildCard(todayScores, yesterdayScores, DiagnosisScoreSnapshot::rednessScore),
                buildCard(todayScores, yesterdayScores, DiagnosisScoreSnapshot::textureScore),
                buildCard(todayScores, yesterdayScores, DiagnosisScoreSnapshot::blemishScore),
                resolveSummaryMessage(course)
        ));
    }

    /** 이전 기록(어제)이 없으면(집중 코스 첫날 등) previous=0, delta=0으로 고정한다. */
    private SkinScoreCard buildCard(
            Optional<DiagnosisScoreSnapshot> todayScores,
            Optional<DiagnosisScoreSnapshot> yesterdayScores,
            Function<DiagnosisScoreSnapshot, Integer> scoreExtractor
    ) {
        int current = todayScores.map(scoreExtractor).orElse(0);
        if (yesterdayScores.isEmpty()) {
            return new SkinScoreCard(current, 0, 0);
        }
        int previous = scoreExtractor.apply(yesterdayScores.get());
        return new SkinScoreCard(current, previous, current - previous);
    }

    /** 이 코스에 등록된 시술 중 가장 최근 시술일 기준, 없으면 코스 시작일 기준으로 경과일수를 계산한다. */
    private String resolveSummaryMessage(CurrentCourseResponse course) {
        LocalDate referenceDate = procedureService.getProceduresForCourse(course.courseId()).stream()
                .map(ProcedureResponse::procedureDate)
                .max(Comparator.naturalOrder())
                .orElse(course.startedAt());

        int dayNumber = Math.max(1, (int) ChronoUnit.DAYS.between(referenceDate, LocalDate.now()) + 1);
        return RecoveryStage.from(dayNumber).buildMessage(dayNumber);
    }
}
