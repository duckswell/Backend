package com.likelion.duckswell.domain.course.controller;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CourseSymptomSummaryResponse;
import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.dto.RecoverySummaryResponse;
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

    @Operation(
            summary = "코스 최근 7일 증상 요약 + 데일리 루틴 추천",
            description = """
                    최근 7일(오늘 포함)간 기록된 증상 중 가장 많이 선택된 키워드 상위 2개(topSymptoms)와,
                    그 증상들을 근거로 추천하는 데일리 루틴 타입(recommendedRoutineTypeCode/Name)을
                    함께 반환합니다(추천만 하며 자동으로 코스를 시작하지는 않습니다 .
                    (실제 시작은 별도 POST /courses/start 호출.)
                    
                    최근 7일 증상 기록이 없으면 topSymptoms는 빈 리스트, 추천 필드는 둘 다 null입니다.
                    """
    )
    ResponseEntity<ApiResponse<CourseSymptomSummaryResponse>> getSymptomSummary(
            @Parameter(description = "조회할 코스 id") Long courseId);

    @Operation(
            summary = "어제 관리 요약 + 오늘 회복 단계",
            description = """
                    집중 코스에서 어제 진행한 관리를 요약하고 현재 회복 단계를 자연스럽게 서술하는
                    정확히 한 문장을 반환합니다(예: "어제 진정 관리를 마치고, 피부 장벽이 회복
                    중이에요"). 
                    
                    어제 완료한 루틴이 없으면 관리 요약 없이 오늘 회복 단계만
                    서술합니다. 회복 일차 숫자는 화면에 별도로 표시되므로 텍스트에는 날짜/일차를
                    언급하지 않습니다. 
                    
                    어제 기록 기준으로 캐싱되어, 같은 날 다시 호출해도 새로
                    생성하지 않고 같은 문장을 반환합니다.
                    """
    )
    ResponseEntity<ApiResponse<RecoverySummaryResponse>> getRecoverySummary(
            @Parameter(description = "조회할 코스 id") Long courseId);
}
