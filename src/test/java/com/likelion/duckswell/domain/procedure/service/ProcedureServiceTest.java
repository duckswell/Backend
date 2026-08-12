package com.likelion.duckswell.domain.procedure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.procedure.dto.ProcedureItemRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.entity.Procedure;
import com.likelion.duckswell.domain.procedure.entity.ProcedureAreaType;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import com.likelion.duckswell.domain.procedure.exception.ProcedureErrorCode;
import com.likelion.duckswell.domain.procedure.repository.ProcedureRepository;
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

@ExtendWith(MockitoExtension.class)
class ProcedureServiceTest {

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private ProcedureService procedureService;

    @Test
    void 집중_코스_진행중이면_시술이_그_코스에_귀속되어_등록된다() {
        // given
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.FOCUS, "집중코스", LocalDate.now(), 0)));
        when(procedureRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcedureRegisterRequest request = new ProcedureRegisterRequest(List.of(
                new ProcedureItemRequest(ProcedureType.SCALING, LocalDate.now(), 1, 3, List.of(ProcedureAreaType.FULL_FACE))
        ));

        // when
        List<ProcedureResponse> result = procedureService.registerProcedures(request);

        // then
        assertThat(result).hasSize(1);

        ArgumentCaptor<Procedure> captor = ArgumentCaptor.forClass(Procedure.class);
        verify(procedureRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(1L);
    }

    @Test
    void 진행중인_코스가_없으면_시술_등록시_예외가_발생한다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.empty());

        ProcedureRegisterRequest request = new ProcedureRegisterRequest(List.of(
                new ProcedureItemRequest(ProcedureType.SCALING, LocalDate.now(), 1, 3, List.of(ProcedureAreaType.FULL_FACE))
        ));

        // when & then
        assertThatThrownBy(() -> procedureService.registerProcedures(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ProcedureErrorCode.FOCUS_COURSE_REQUIRED);
    }

    @Test
    void 데일리_코스_진행중이면_시술_등록시_예외가_발생한다() {
        // given
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.DAILY, "수분 보충 케어", LocalDate.now(), 0)));

        ProcedureRegisterRequest request = new ProcedureRegisterRequest(List.of(
                new ProcedureItemRequest(ProcedureType.SCALING, LocalDate.now(), 1, 3, List.of(ProcedureAreaType.FULL_FACE))
        ));

        // when & then
        assertThatThrownBy(() -> procedureService.registerProcedures(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ProcedureErrorCode.FOCUS_COURSE_REQUIRED);
    }

    @Test
    void 집중_코스_진행중이면_현재_코스_시술만_조회된다() {
        // given
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.FOCUS, "집중코스", LocalDate.now(), 0)));
        when(procedureRepository.findByMemberIdAndCourseIdOrderByProcedureDateDesc(anyLong(), eq(1L)))
                .thenReturn(List.of());

        // when
        List<ProcedureResponse> result = procedureService.getCurrentCourseProcedures();

        // then
        assertThat(result).isEmpty();
        verify(procedureRepository).findByMemberIdAndCourseIdOrderByProcedureDateDesc(anyLong(), eq(1L));
    }

    @Test
    void 데일리_코스_진행중이면_현재_코스_시술_조회시_빈_목록을_반환한다() {
        // given
        when(courseService.getCurrentCourse())
                .thenReturn(Optional.of(new CurrentCourseResponse(1L, CourseType.DAILY, "수분 보충 케어", LocalDate.now(), 0)));

        // when
        List<ProcedureResponse> result = procedureService.getCurrentCourseProcedures();

        // then
        assertThat(result).isEmpty();
        verify(procedureRepository, never()).findByMemberIdAndCourseIdOrderByProcedureDateDesc(any(), any());
    }

    @Test
    void 진행중인_코스가_없으면_현재_코스_시술_조회시_빈_목록을_반환한다() {
        // given
        when(courseService.getCurrentCourse()).thenReturn(Optional.empty());

        // when
        List<ProcedureResponse> result = procedureService.getCurrentCourseProcedures();

        // then
        assertThat(result).isEmpty();
    }
}
