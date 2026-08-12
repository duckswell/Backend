package com.likelion.duckswell.domain.dashboard.client.llm;

import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import java.time.LocalDate;
import java.util.List;

/** FOCUS는 procedures/recentRoutines를, DAILY는 weather/recentRoutines를 근거로 사용한다. today는 procedureDate로부터 회복 경과일을 계산하는 기준. */
public record LlmChecklistContext(
        CourseType courseType,
        LocalDate today,
        List<ProcedureResponse> procedures,
        List<RoutineSnapshot> recentRoutines,
        WeatherResponse weather
) {
}
