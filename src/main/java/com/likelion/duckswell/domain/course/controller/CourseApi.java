package com.likelion.duckswell.domain.course.controller;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Course", description = "집중/데일리 코스 시작·종료 API")
public interface CourseApi {

    @Operation(
            summary = "코스 시작",
            description = """
                    집중 코스 또는 데일리 코스를 새로 시작합니다. 이미 진행 중인 코스가 있으면 실패합니다.

                    courseType: FOCUS(집중) | DAILY(데일리)

                    routineTypeCode: COOLDOWN(쿨다운) | CLEAR_UP(클리어업) | SEBUM_CONTROL(피지컨트롤) | HYDRATION(수분충전)
                    - 집중 코스(FOCUS)는 routineTypeCode를 보낼 수 없습니다(생략 또는 null).
                    - 데일리 코스(DAILY)는 routineTypeCode를 반드시 지정해야 합니다.
                    """
    )
    ResponseEntity<ApiResponse<CourseResponse>> startCourse(@Valid CourseStartRequest request);

    @Operation(
            summary = "코스 종료",
            description = "진행 중인 코스를 종료 처리합니다(ended_at 세팅, 상태 COMPLETED로 전환)."
    )
    ResponseEntity<ApiResponse<CourseResponse>> endCourse(@Parameter(description = "종료할 코스 id") Long courseId);

    @Operation(
            summary = "집중 코스로 돌아가기 (테스트용)",
            description = """
                    진행 중인 코스가 있으면 종료하고, 새 집중 코스를 시작합니다.
                    테스트/데모 편의를 위해 실제 서비스에도 노출되는 버튼입니다.
                    """
    )
    ResponseEntity<ApiResponse<CourseResponse>> restartFocusCourse();

    @Operation(
            summary = "지난 코스 기록 조회",
            description = "마이페이지에서 보여줄, 시작~종료일 기준 지난 코스 목록을 최신순으로 반환합니다."
    )
    ResponseEntity<ApiResponse<List<CourseResponse>>> getCourseHistory();

    @Operation(
            summary = "현재 진행중인 코스 상태 조회",
            description = """
                    현재 진행중인 코스와 연속 진행일수(streakDays)를 반환합니다.
                    집중 코스는 label이 "집중코스"로 고정되고, 데일리 코스는 routineType 이름
                    (예: 수분 보충 케어, 피지 조절 케어)이 label로 내려갑니다.

                    연속일수는 오늘 루틴을 아직 완료하지 않았어도 스트릭이 끊기지 않고(어제까지 기준으로 셈),
                    루틴을 완료하지 않은 날을 만나면 그 지점에서 끊깁니다.
                    진행중인 코스가 없으면 data 없이 응답합니다.
                    """
    )
    ResponseEntity<ApiResponse<CurrentCourseResponse>> getCurrentCourse();
}
