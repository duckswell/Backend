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
@Transactional(readOnly = true)
public class ChecklistService {

    private static final int RECENT_ROUTINE_LOOKBACK = 3;

    private final ChecklistItemRepository checklistItemRepository;
    private final CourseService courseService;
    private final ProcedureService procedureService;
    private final RoutineService routineService;
    private final WeatherService weatherService;
    private final LlmChecklistClient llmChecklistClient;

    /** 오늘 이미 생성된 항목이 있으면 재사용하고, 없을 때만 진행 중인 코스 기준으로 새로 생성한다. */
    @Transactional
    public List<ChecklistItemResponse> getTodayChecklist(Double lat, Double lon) {
        LocalDate today = LocalDate.now();
        List<ChecklistItem> existingItems = checklistItemRepository.findByMemberIdAndItemDate(Member.DEFAULT_ID, today);
        if (!existingItems.isEmpty()) {
            return existingItems.stream().map(ChecklistItemResponse::from).toList();
        }
        return generateTodayChecklist(lat, lon, today);
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

    private List<ChecklistItemResponse> generateTodayChecklist(Double lat, Double lon, LocalDate today) {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty()) {
            return List.of();
        }

        CurrentCourseResponse course = currentCourse.get();
        LlmChecklistResult result = llmChecklistClient.generate(buildContext(course, lat, lon));
        ChecklistSourceType sourceType = resolveSourceType(course.courseType());

        List<ChecklistItem> savedItems = result.items().stream()
                .map(draft -> checklistItemRepository.save(
                        new ChecklistItem(Member.DEFAULT_ID, today, draft.title(), draft.description(), sourceType)))
                .toList();

        return savedItems.stream().map(ChecklistItemResponse::from).toList();
    }

    private LlmChecklistContext buildContext(CurrentCourseResponse course, Double lat, Double lon) {
        List<RoutineSnapshot> recentRoutines = routineService.getRecentRoutineSnapshots(course.courseId(), RECENT_ROUTINE_LOOKBACK);

        if (course.courseType() == CourseType.FOCUS) {
            return new LlmChecklistContext(CourseType.FOCUS, procedureService.getMyProcedures(), recentRoutines, null);
        }
        return new LlmChecklistContext(CourseType.DAILY, null, recentRoutines, weatherService.getCurrentWeather(lat, lon));
    }

    private ChecklistSourceType resolveSourceType(CourseType courseType) {
        return courseType == CourseType.FOCUS ? ChecklistSourceType.PROCEDURE_CAUTION : ChecklistSourceType.WEATHER_ROUTINE;
    }

    private ChecklistItem getOwnedChecklistItem(Long checklistItemId) {
        return checklistItemRepository.findByIdAndMemberId(checklistItemId, Member.DEFAULT_ID)
                .orElseThrow(() -> new CustomException(DashboardErrorCode.CHECKLIST_ITEM_NOT_FOUND));
    }
}
