package com.likelion.duckswell.domain.course.service;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.exception.CourseErrorCode;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final RoutineTypeRepository routineTypeRepository;
    private final RoutineRepository routineRepository;

    @Transactional
    public CourseResponse startCourse(CourseStartRequest request) {
        courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .ifPresent(course -> {
                    throw new CustomException(CourseErrorCode.ACTIVE_COURSE_ALREADY_EXISTS);
                });

        RoutineType routineType = resolveRoutineTypeForStart(request.courseType(), request.routineTypeCode());

        Course course = new Course(Member.DEFAULT_ID, null, request.courseType(), routineType, LocalDate.now());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse endCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        if (course.getStatus() != CourseStatus.IN_PROGRESS) {
            throw new CustomException(CourseErrorCode.COURSE_ALREADY_ENDED);
        }
        course.end(LocalDate.now());
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse restartFocusCourse() {
        Optional<Course> activeCourse = courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS);
        activeCourse.ifPresent(course -> course.end(LocalDate.now()));

        Course newCourse = new Course(Member.DEFAULT_ID, null, CourseType.FOCUS, null, LocalDate.now());
        return CourseResponse.from(courseRepository.save(newCourse));
    }

    public List<CourseResponse> getCourseHistory() {
        return courseRepository.findByMemberIdOrderByStartedAtDescIdDesc(Member.DEFAULT_ID).stream()
                .map(CourseResponse::from)
                .toList();
    }

    /** 다른 도메인(diagnosis 등)이 courseId만으로 코스 정보를 참조해야 할 때 쓰는 조회용 메서드. */
    public CourseResponse getCourse(Long courseId) {
        return CourseResponse.from(getCourseOrThrow(courseId));
    }

    public Optional<CurrentCourseResponse> getCurrentCourse() {
        return courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .map(course -> CurrentCourseResponse.of(course, calculateStreakDays(course.getId())));
    }

    /** 다른 도메인(routine 등)이 courseId만으로 연속 지속일을 참조해야 할 때 쓰는 조회용 메서드. */
    public int getStreakDays(Long courseId) {
        return calculateStreakDays(courseId);
    }

    private int calculateStreakDays(Long courseId) {
        Set<LocalDate> completedDates = routineRepository.findByCourseIdOrderByRoutineDateDesc(courseId).stream()
                .map(Routine::getCompletedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        LocalDate cursor = LocalDate.now();
        if (!completedDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }

        int streakDays = 0;
        while (completedDates.contains(cursor)) {
            streakDays++;
            cursor = cursor.minusDays(1);
        }
        return streakDays;
    }

    private RoutineType resolveRoutineTypeForStart(CourseType courseType, RoutineTypeCode routineTypeCode) {
        if (courseType == CourseType.FOCUS) {
            if (routineTypeCode != null) {
                throw new CustomException(CourseErrorCode.ROUTINE_TYPE_NOT_ALLOWED_FOR_FOCUS);
            }
            return null;
        }

        if (routineTypeCode == null) {
            throw new CustomException(CourseErrorCode.ROUTINE_TYPE_REQUIRED_FOR_DAILY);
        }
        return routineTypeRepository.findById(routineTypeCode)
                .orElseThrow(() -> new CustomException(CourseErrorCode.ROUTINE_TYPE_NOT_FOUND));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
    }
}
