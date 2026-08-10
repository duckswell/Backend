package com.likelion.duckswell.domain.course.dto;

import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import jakarta.validation.constraints.NotNull;

public record CourseStartRequest(
        @NotNull CourseType courseType,
        RoutineTypeCode routineTypeCode
) {
}
