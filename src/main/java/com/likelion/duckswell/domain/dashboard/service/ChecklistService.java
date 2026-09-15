package com.likelion.duckswell.domain.dashboard.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistClient;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistContext;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult.ChecklistItemDraft;
import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistSourceType;
import com.likelion.duckswell.domain.dashboard.exception.DashboardErrorCode;
import com.likelion.duckswell.domain.dashboard.repository.ChecklistItemRepository;
import com.likelion.duckswell.domain.member.auth.CurrentMemberContext;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
     * 아직 코스를 시작하지 않았다면(주로 게스트 최초 진입) {@link #getOrGenerateDefaultChecklist}로
     * 날씨 기반 기본 체크리스트를 대신 반환한다.
     *
     * 의도적으로 이 메서드엔 @Transactional을 걸지 않는다 - 캐시 조회/저장 사이에 WeatherClient(최대
     * 10초)·LlmChecklistClient(최대 60초) 외부 호출이 끼어 있어서, 메서드 전체를 하나의 트랜잭션으로
     * 묶으면 그 시간만큼 DB 커넥션을 점유해 커넥션 풀 고갈로 이어질 수 있다. 대신 courseService/
     * routineService/procedureService(각각 클래스 레벨 @Transactional(readOnly=true))와
     * checklistItemRepository의 개별 호출이 각자 짧은 트랜잭션을 갖도록 둔다.
     */
    public List<ChecklistItemResponse> getTodayChecklist(Double lat, Double lon) {
        LocalDate today = LocalDate.now();
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty()) {
            return getOrGenerateDefaultChecklist(lat, lon, today);
        }

        CurrentCourseResponse course = currentCourse.get();
        List<ChecklistItem> existingItems = checklistItemRepository.findByMemberIdAndCourseIdAndItemDateOrderByItemOrderAsc(
                CurrentMemberContext.getMemberId(), course.courseId(), today);
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

    /**
     * 저장은 saveAll로 한 번에 묶어 두 항목이 함께 성공/실패하도록 한다. 각 항목엔 배치 내 순번
     * (itemOrder: 0, 1)을 매기는데, 이 값은 LLM 응답 문구와 무관하게 고정돼 있어서 - 동시 요청
     * 두 건이 같은 (memberId, courseId, itemDate)에 대해 동시에 이 시점까지 왔다면(생성된 문구가
     * 서로 다르더라도) 유니크 제약(member_id, course_id, item_date, item_order) 위반으로 둘 중
     * 하나는 반드시 저장에 실패한다 - 그 경우 LLM을 다시 호출하지 않고, 먼저 저장에 성공한 요청의
     * 결과를 다시 조회해 반환한다.
     */
    private List<ChecklistItemResponse> generateTodayChecklist(CurrentCourseResponse course, Double lat, Double lon, LocalDate today) {
        Long memberId = CurrentMemberContext.getMemberId();
        LlmChecklistResult result = llmChecklistClient.generate(buildContext(course, lat, lon, today));
        ChecklistSourceType sourceType = resolveSourceType(course.courseType());

        List<ChecklistItem> itemsToSave = toChecklistItems(memberId, course.courseId(), today, result, sourceType);
        return saveItemsOrFetchExisting(memberId, course.courseId(), today, itemsToSave);
    }

    /**
     * 아직 코스를 시작하지 않은 회원(주로 게스트 최초 진입)을 위한 기본 체크리스트.
     * 코스가 없어 진짜 courseId가 없으므로 {@link ChecklistItem#NO_COURSE_ID}를 대신 키로 써서,
     * 기존 (memberId, courseId, itemDate, itemOrder) 유니크 제약과 동시 생성 충돌 처리를 그대로
     * 재사용한다 - 실제 날씨(위경도 기반)를 그대로 근거로 쓰되, 시술/루틴 이력만 없는 것이다.
     */
    private List<ChecklistItemResponse> getOrGenerateDefaultChecklist(Double lat, Double lon, LocalDate today) {
        Long memberId = CurrentMemberContext.getMemberId();
        List<ChecklistItem> existingItems = checklistItemRepository.findByMemberIdAndCourseIdAndItemDateOrderByItemOrderAsc(
                memberId, ChecklistItem.NO_COURSE_ID, today);
        if (!existingItems.isEmpty()) {
            return existingItems.stream().map(ChecklistItemResponse::from).toList();
        }

        WeatherResponse weather = weatherService.getTodayForecast(lat, lon);
        LlmChecklistResult result = llmChecklistClient.generateDefault(today, weather);
        List<ChecklistItem> itemsToSave = toChecklistItems(
                memberId, ChecklistItem.NO_COURSE_ID, today, result, ChecklistSourceType.WEATHER_DEFAULT);
        return saveItemsOrFetchExisting(memberId, ChecklistItem.NO_COURSE_ID, today, itemsToSave);
    }

    private List<ChecklistItem> toChecklistItems(
            Long memberId, Long courseId, LocalDate today, LlmChecklistResult result, ChecklistSourceType sourceType) {
        return IntStream.range(0, result.items().size())
                .mapToObj(index -> {
                    ChecklistItemDraft draft = result.items().get(index);
                    return new ChecklistItem(memberId, courseId, today, index, draft.title(), draft.description(), sourceType);
                })
                .toList();
    }

    /**
     * 저장은 saveAll로 한 번에 묶어 두 항목이 함께 성공/실패하도록 한다. 각 항목엔 배치 내 순번
     * (itemOrder: 0, 1)을 매기는데, 이 값은 LLM 응답 문구와 무관하게 고정돼 있어서 - 동시 요청
     * 두 건이 같은 (memberId, courseId, itemDate)에 대해 동시에 이 시점까지 왔다면(생성된 문구가
     * 서로 다르더라도) 유니크 제약(member_id, course_id, item_date, item_order) 위반으로 둘 중
     * 하나는 반드시 저장에 실패한다 - 그 경우 LLM을 다시 호출하지 않고, 먼저 저장에 성공한 요청의
     * 결과를 다시 조회해 반환한다.
     */
    private List<ChecklistItemResponse> saveItemsOrFetchExisting(
            Long memberId, Long courseId, LocalDate today, List<ChecklistItem> itemsToSave) {
        try {
            List<ChecklistItem> savedItems = checklistItemRepository.saveAll(itemsToSave);
            return savedItems.stream().map(ChecklistItemResponse::from).toList();
        } catch (DataIntegrityViolationException e) {
            log.warn("체크리스트 동시 생성 충돌 감지 - 기존 항목을 재조회합니다 (memberId={}, courseId={}, itemDate={})",
                    memberId, courseId, today);
            List<ChecklistItem> existingItems = checklistItemRepository.findByMemberIdAndCourseIdAndItemDateOrderByItemOrderAsc(
                    memberId, courseId, today);
            return existingItems.stream().map(ChecklistItemResponse::from).toList();
        }
    }

    private LlmChecklistContext buildContext(CurrentCourseResponse course, Double lat, Double lon, LocalDate today) {
        List<RoutineSnapshot> recentRoutines = routineService.getRecentRoutineSnapshots(course.courseId(), RECENT_ROUTINE_LOOKBACK);

        if (course.courseType() == CourseType.FOCUS) {
            return new LlmChecklistContext(
                    CourseType.FOCUS, today, procedureService.getProceduresForCourse(course.courseId()), null, recentRoutines, null);
        }
        return new LlmChecklistContext(
                CourseType.DAILY, today, null, course.label(), recentRoutines, weatherService.getTodayForecast(lat, lon));
    }

    private ChecklistSourceType resolveSourceType(CourseType courseType) {
        return courseType == CourseType.FOCUS ? ChecklistSourceType.PROCEDURE_CAUTION : ChecklistSourceType.WEATHER_ROUTINE;
    }

    private ChecklistItem getOwnedChecklistItem(Long checklistItemId) {
        return checklistItemRepository.findByIdAndMemberId(checklistItemId, CurrentMemberContext.getMemberId())
                .orElseThrow(() -> new CustomException(DashboardErrorCode.CHECKLIST_ITEM_NOT_FOUND));
    }
}
