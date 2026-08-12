package com.likelion.duckswell.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistClient;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult;
import com.likelion.duckswell.domain.dashboard.client.llm.LlmChecklistResult.ChecklistItemDraft;
import com.likelion.duckswell.domain.dashboard.dto.ChecklistItemResponse;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistSourceType;
import com.likelion.duckswell.domain.dashboard.exception.DashboardErrorCode;
import com.likelion.duckswell.domain.dashboard.repository.ChecklistItemRepository;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.service.WeatherService;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private ProcedureService procedureService;

    @Mock
    private RoutineService routineService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private LlmChecklistClient llmChecklistClient;

    @InjectMocks
    private ChecklistService checklistService;

    @Test
    void 진행중인_코스가_없으면_빈_리스트를_반환하고_LLM을_호출하지_않는다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.empty());

        // when
        List<ChecklistItemResponse> result = checklistService.getTodayChecklist(null, null);

        // then
        assertThat(result).isEmpty();
        verify(llmChecklistClient, never()).generate(any());
    }

    @Test
    void 오늘_같은_코스로_이미_생성된_항목이_있으면_그대로_반환하고_LLM을_호출하지_않는다() {
        // given
        ChecklistItem existing1 = new ChecklistItem(1L, 1L, LocalDate.now(), "제목1", "설명1", ChecklistSourceType.WEATHER_ROUTINE);
        ChecklistItem existing2 = new ChecklistItem(1L, 1L, LocalDate.now(), "제목2", "설명2", ChecklistSourceType.WEATHER_ROUTINE);
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.DAILY, "수분 보충 케어", LocalDate.now(), 0)));
        when(checklistItemRepository.findByMemberIdAndCourseIdAndItemDate(anyLong(), anyLong(), any()))
                .thenReturn(List.of(existing1, existing2));

        // when
        List<ChecklistItemResponse> result = checklistService.getTodayChecklist(null, null);

        // then
        assertThat(result).extracting(ChecklistItemResponse::title).containsExactly("제목1", "제목2");
        verify(llmChecklistClient, never()).generate(any());
    }

    @Test
    void FOCUS_코스면_시술_내역_기반으로_체크리스트를_생성하고_PROCEDURE_CAUTION으로_저장한다() {
        // given
        when(checklistItemRepository.findByMemberIdAndCourseIdAndItemDate(anyLong(), anyLong(), any())).thenReturn(List.of());
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.FOCUS, "집중코스", LocalDate.now(), 0)));
        when(procedureService.getMyProcedures()).thenReturn(List.of());
        when(routineService.getRecentRoutineSnapshots(anyLong(), anyInt())).thenReturn(List.of());
        when(llmChecklistClient.generate(any())).thenReturn(new LlmChecklistResult(List.of(
                new ChecklistItemDraft("시술 부위에 손대지 않기", "자극이 되지 않도록 시술 부위를 만지거나 문지르지 마세요."),
                new ChecklistItemDraft("각질은 억지로 떼지 않기", "일어난 각질이나 딱지는 손으로 뜯지 말고 자연스럽게 떨어지도록 두세요.")
        )));
        when(checklistItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<ChecklistItemResponse> result = checklistService.getTodayChecklist(null, null);

        // then
        assertThat(result).extracting(ChecklistItemResponse::title)
                .containsExactly("시술 부위에 손대지 않기", "각질은 억지로 떼지 않기");
        verify(weatherService, never()).getTodayForecast(any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(checklistItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(item ->
                assertThat(item.getSourceType()).isEqualTo(ChecklistSourceType.PROCEDURE_CAUTION));
    }

    @Test
    void DAILY_코스면_날씨_기반으로_체크리스트를_생성하고_WEATHER_ROUTINE으로_저장한다() {
        // given
        when(checklistItemRepository.findByMemberIdAndCourseIdAndItemDate(anyLong(), anyLong(), any())).thenReturn(List.of());
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.DAILY, "수분 보충 케어", LocalDate.now(), 0)));
        when(routineService.getRecentRoutineSnapshots(anyLong(), anyInt())).thenReturn(List.of());
        when(weatherService.getTodayForecast(null, null))
                .thenReturn(new WeatherResponse(20.0, "Sunny", 20, 8.0, 10.0, 10.0, 1));
        when(llmChecklistClient.generate(any())).thenReturn(new LlmChecklistResult(List.of(
                new ChecklistItemDraft("세안 후 보습제 충분히 바르기", "날씨가 건조하므로 오늘은 특히 충분한 보습이 필요해요."),
                new ChecklistItemDraft("틈틈히 자외선 차단제 바르기", "자외선 지수가 높아 여드름 흔적에 색소침착이 되기 쉬워요.")
        )));
        when(checklistItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<ChecklistItemResponse> result = checklistService.getTodayChecklist(null, null);

        // then
        assertThat(result).extracting(ChecklistItemResponse::title)
                .containsExactly("세안 후 보습제 충분히 바르기", "틈틈히 자외선 차단제 바르기");
        verify(procedureService, never()).getMyProcedures();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(checklistItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(item ->
                assertThat(item.getSourceType()).isEqualTo(ChecklistSourceType.WEATHER_ROUTINE));
    }

    @Test
    void 동시_생성으로_유니크_제약_위반이_발생하면_LLM을_다시_호출하지_않고_기존_항목을_재조회한다() {
        // given
        ChecklistItem winnerItem1 = new ChecklistItem(1L, 1L, LocalDate.now(), "제목1", "설명1", ChecklistSourceType.WEATHER_ROUTINE);
        ChecklistItem winnerItem2 = new ChecklistItem(1L, 1L, LocalDate.now(), "제목2", "설명2", ChecklistSourceType.WEATHER_ROUTINE);
        when(checklistItemRepository.findByMemberIdAndCourseIdAndItemDate(anyLong(), anyLong(), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(winnerItem1, winnerItem2));
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.DAILY, "수분 보충 케어", LocalDate.now(), 0)));
        when(routineService.getRecentRoutineSnapshots(anyLong(), anyInt())).thenReturn(List.of());
        when(weatherService.getTodayForecast(null, null))
                .thenReturn(new WeatherResponse(20.0, "Sunny", 20, 8.0, 10.0, 10.0, 1));
        when(llmChecklistClient.generate(any())).thenReturn(new LlmChecklistResult(List.of(
                new ChecklistItemDraft("제목1", "설명1"),
                new ChecklistItemDraft("제목2", "설명2")
        )));
        when(checklistItemRepository.saveAll(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        // when
        List<ChecklistItemResponse> result = checklistService.getTodayChecklist(null, null);

        // then
        assertThat(result).extracting(ChecklistItemResponse::title).containsExactly("제목1", "제목2");
        verify(llmChecklistClient, times(1)).generate(any());
    }

    @Test
    void 체크되지_않은_항목을_토글하면_체크된다() {
        // given
        ChecklistItem item = new ChecklistItem(1L, 1L, LocalDate.now(), "제목", "설명", ChecklistSourceType.WEATHER_ROUTINE);
        when(checklistItemRepository.findByIdAndMemberId(anyLong(), anyLong())).thenReturn(Optional.of(item));

        // when
        ChecklistItemResponse response = checklistService.toggleCheck(1L);

        // then
        assertThat(response.checked()).isTrue();
    }

    @Test
    void 체크된_항목을_토글하면_체크가_취소된다() {
        // given
        ChecklistItem item = new ChecklistItem(1L, 1L, LocalDate.now(), "제목", "설명", ChecklistSourceType.WEATHER_ROUTINE);
        item.check();
        when(checklistItemRepository.findByIdAndMemberId(anyLong(), anyLong())).thenReturn(Optional.of(item));

        // when
        ChecklistItemResponse response = checklistService.toggleCheck(1L);

        // then
        assertThat(response.checked()).isFalse();
    }

    @Test
    void 존재하지_않는_항목을_토글하면_예외가_발생한다() {
        // given
        when(checklistItemRepository.findByIdAndMemberId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> checklistService.toggleCheck(999L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(DashboardErrorCode.CHECKLIST_ITEM_NOT_FOUND);
    }
}
