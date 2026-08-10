package com.likelion.duckswell.domain.course.service;

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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final RoutineTypeRepository routineTypeRepository;

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
