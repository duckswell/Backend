package com.likelion.duckswell.domain.course.dto;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseType;
import java.time.LocalDate;

public record CurrentCourseResponse(
        Long courseId,
        CourseType courseType,
        String label,
        LocalDate startedAt,
        int streakDays
) {
    public static CurrentCourseResponse of(Course course, int streakDays) {
        String label = course.getCourseType() == CourseType.FOCUS
                ? "집중코스"
                : course.getRoutineType().getName();
        return new CurrentCourseResponse(course.getId(), course.getCourseType(), label, course.getStartedAt(), streakDays);
    }
}
