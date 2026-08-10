package com.likelion.duckswell.domain.course.dto;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import java.time.LocalDate;

public record CourseResponse(
        Long id,
        CourseType courseType,
        RoutineTypeCode routineTypeCode,
        String routineTypeName,
        LocalDate startedAt,
        LocalDate endedAt,
        CourseStatus status
) {
    public static CourseResponse from(Course course) {
        boolean hasRoutineType = course.getRoutineType() != null;
        return new CourseResponse(
                course.getId(),
                course.getCourseType(),
                hasRoutineType ? course.getRoutineType().getCode() : null,
                hasRoutineType ? course.getRoutineType().getName() : null,
                course.getStartedAt(),
                course.getEndedAt(),
                course.getStatus()
        );
    }
}
