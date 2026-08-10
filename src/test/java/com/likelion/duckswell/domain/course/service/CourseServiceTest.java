package com.likelion.duckswell.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.exception.CourseErrorCode;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CourseService.class)
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoutineTypeRepository routineTypeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 집중_코스를_시작하면_루틴타입_없이_진행중_상태로_생성된다() {
        // given
        endAnyActiveCourse();

        // when
        CourseResponse response = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // then
        assertThat(response.courseType()).isEqualTo(CourseType.FOCUS);
        assertThat(response.routineTypeCode()).isNull();
        assertThat(response.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void 집중_코스를_시작할_때_루틴타입을_같이_보내면_거부된다() {
        // given
        endAnyActiveCourse();

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, RoutineTypeCode.COOLDOWN)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ROUTINE_TYPE_NOT_ALLOWED_FOR_FOCUS);
    }

    @Test
    void 진행중인_코스가_있으면_새_코스를_시작할_수_없다() {
        // given
        endAnyActiveCourse();
        courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ACTIVE_COURSE_ALREADY_EXISTS);
    }

    @Test
    void 데일리_코스는_루틴타입을_지정하지_않으면_시작할_수_없다() {
        // given
        endAnyActiveCourse();

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.DAILY, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ROUTINE_TYPE_REQUIRED_FOR_DAILY);
    }

    // ROUTINE_TYPE_NOT_FOUND(존재하지 않는 루틴타입) 분기는 이 테스트 스위트에서 자동으로 검증하지 못한다:
    // routine_type은 4개 코드가 항상 전부 시드되어 있고 여러 테이블이 FK로 물려 있어,
    // 특정 코드를 안전하게 "없는 상태"로 만들 방법이 없다(Mockito 등 모킹 의존성도 없음).
    // startCourse/analyze.py 수동 curl 검증(2026-08-10) 때 시드 전 상태에서 CO006으로 정상 동작 확인함.

    @Test
    void 데일리_코스를_시작하면_지정한_루틴타입이_반영된다() {
        // given
        endAnyActiveCourse();
        seedRoutineType(RoutineTypeCode.HYDRATION);

        // when
        CourseResponse response = courseService.startCourse(new CourseStartRequest(CourseType.DAILY, RoutineTypeCode.HYDRATION));

        // then
        assertThat(response.routineTypeCode()).isEqualTo(RoutineTypeCode.HYDRATION);
        assertThat(response.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void 진행중인_코스를_종료하면_completed_상태가_된다() {
        // given
        endAnyActiveCourse();
        CourseResponse started = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when
        CourseResponse ended = courseService.endCourse(started.id());

        // then
        assertThat(ended.status()).isEqualTo(CourseStatus.COMPLETED);
        assertThat(ended.endedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void 이미_종료된_코스는_다시_종료할_수_없다() {
        // given
        endAnyActiveCourse();
        CourseResponse started = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));
        courseService.endCourse(started.id());

        // when & then
        assertThatThrownBy(() -> courseService.endCourse(started.id()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.COURSE_ALREADY_ENDED);
    }

    @Test
    void 집중_코스로_돌아가면_기존_진행중_코스는_종료되고_새_집중_코스가_생긴다() {
        // given
        endAnyActiveCourse();
        seedRoutineType(RoutineTypeCode.COOLDOWN);
        CourseResponse daily = courseService.startCourse(new CourseStartRequest(CourseType.DAILY, RoutineTypeCode.COOLDOWN));

        // when
        CourseResponse restarted = courseService.restartFocusCourse();

        // then
        assertThat(restarted.courseType()).isEqualTo(CourseType.FOCUS);
        assertThat(restarted.routineTypeCode()).isNull();
        Course reloadedDaily = courseRepository.findById(daily.id()).orElseThrow();
        assertThat(reloadedDaily.getStatus()).isEqualTo(CourseStatus.COMPLETED);
    }

    @Test
    void 코스_기록_조회에_종료된_코스와_진행중인_코스가_모두_포함된다() {
        // given
        endAnyActiveCourse();
        CourseResponse first = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));
        courseService.endCourse(first.id());
        CourseResponse second = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when
        List<CourseResponse> history = courseService.getCourseHistory();

        // then: 같은 날 생성돼 startedAt이 동일할 수 있어 순서(index)가 아닌 id 기준으로 상태를 확인한다
        assertThat(history).extracting(CourseResponse::id).contains(first.id(), second.id());
        assertThat(history).filteredOn(c -> c.id().equals(first.id()))
                .extracting(CourseResponse::status)
                .containsExactly(CourseStatus.COMPLETED);
        assertThat(history).filteredOn(c -> c.id().equals(second.id()))
                .extracting(CourseResponse::status)
                .containsExactly(CourseStatus.IN_PROGRESS);
    }

    private void seedRoutineType(RoutineTypeCode code) {
        if (routineTypeRepository.existsById(code)) {
            return;
        }
        routineTypeRepository.save(new RoutineType(code, code.name(), null, null));
    }

    /** 시드 데이터 등으로 이미 진행 중인 코스가 있으면 종료해서, 각 테스트가 "진행 중인 코스 없음" 상태에서 시작하도록 만든다. */
    private void endAnyActiveCourse() {
        courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .ifPresent(course -> course.end(LocalDate.now()));
        entityManager.flush();
    }
}
