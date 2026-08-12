package com.likelion.duckswell.domain.dashboard.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistClient;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistContext;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult;
import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistSourceType;
import com.likelion.duckswell.domain.dashboard.exception.DashboardErrorCode;
import com.likelion.duckswell.domain.dashboard.repository.ChecklistItemRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private static final int RECENT_ROUTINE_LOOKBACK = 3;

    private final ChecklistItemRepository checklistItemRepository;
    private final CourseService courseService;
    private final ProcedureService procedureService;
    private final RoutineService routineService;
    private final WeatherService weatherService;
    private final LlmChecklistClient llmChecklistClient;

    /**
     * 오늘, 그리고 지금 진행 중인 코스 기준으로 이미 생성된 항목이 있으면 재사용하고, 없을 때만 새로 생성한다.
     * 코스가 바뀌면(FOCUS↔DAILY 전환, 코스 재시작 등) 같은 날이어도 courseId가 달라져 곧바로 새로 생성된다.
     *
     * 의도적으로 이 메서드엔 @Transactional을 걸지 않는다 - 캐시 조회/저장 사이에 WeatherClient(최대
     * 10초)·LlmChecklistClient(최대 60초) 외부 호출이 끼어 있어서, 메서드 전체를 하나의 트랜잭션으로
     * 묶으면 그 시간만큼 DB 커넥션을 점유해 커넥션 풀 고갈로 이어질 수 있다. 대신 courseService/
     * routineService/procedureService(각각 클래스 레벨 @Transactional(readOnly=true))와
     * checklistItemRepository의 개별 호출이 각자 짧은 트랜잭션을 갖도록 둔다.
     */
    public List<ChecklistItemResponse> getTodayChecklist(Double lat, Double lon) {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty()) {
            return List.of();
        }

        CurrentCourseResponse course = currentCourse.get();
        LocalDate today = LocalDate.now();
        List<ChecklistItem> existingItems = checklistItemRepository.findByMemberIdAndCourseIdAndItemDate(
                Member.DEFAULT_ID, course.courseId(), today);
        if (!existingItems.isEmpty()) {
            return existingItems.stream().map(ChecklistItemResponse::from).toList();
        }
        return generateTodayChecklist(course, lat, lon, today);
    }

    @Transactional
    public ChecklistItemResponse toggleCheck(Long checklistItemId) {
        ChecklistItem checklistItem = getOwnedChecklistItem(checklistItemId);
        if (checklistItem.isChecked()) {
            checklistItem.uncheck();
        } else {
            checklistItem.check();
        }
        return ChecklistItemResponse.from(checklistItem);
    }

    private List<ChecklistItemResponse> generateTodayChecklist(CurrentCourseResponse course, Double lat, Double lon, LocalDate today) {
        LlmChecklistResult result = llmChecklistClient.generate(buildContext(course, lat, lon, today));
        ChecklistSourceType sourceType = resolveSourceType(course.courseType());

        List<ChecklistItem> savedItems = result.items().stream()
                .map(draft -> checklistItemRepository.save(
                        new ChecklistItem(Member.DEFAULT_ID, course.courseId(), today, draft.title(), draft.description(), sourceType)))
                .toList();

        return savedItems.stream().map(ChecklistItemResponse::from).toList();
    }

    private LlmChecklistContext buildContext(CurrentCourseResponse course, Double lat, Double lon, LocalDate today) {
        List<RoutineSnapshot> recentRoutines = routineService.getRecentRoutineSnapshots(course.courseId(), RECENT_ROUTINE_LOOKBACK);

        if (course.courseType() == CourseType.FOCUS) {
            return new LlmChecklistContext(CourseType.FOCUS, today, procedureService.getMyProcedures(), null, recentRoutines, null);
        }
        return new LlmChecklistContext(
                CourseType.DAILY, today, null, course.label(), recentRoutines, weatherService.getTodayForecast(lat, lon));
    }

    private ChecklistSourceType resolveSourceType(CourseType courseType) {
        return courseType == CourseType.FOCUS ? ChecklistSourceType.PROCEDURE_CAUTION : ChecklistSourceType.WEATHER_ROUTINE;
    }

    private ChecklistItem getOwnedChecklistItem(Long checklistItemId) {
        return checklistItemRepository.findByIdAndMemberId(checklistItemId, Member.DEFAULT_ID)
                .orElseThrow(() -> new CustomException(DashboardErrorCode.CHECKLIST_ITEM_NOT_FOUND));
    }
}
