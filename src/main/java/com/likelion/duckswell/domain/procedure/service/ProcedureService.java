package com.likelion.duckswell.domain.procedure.service;

import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.procedure.dto.ProcedureItemRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.entity.Procedure;
import com.likelion.duckswell.domain.procedure.exception.ProcedureErrorCode;
import com.likelion.duckswell.domain.procedure.repository.ProcedureRepository;
import com.likelion.duckswell.global.exception.CustomException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final CourseService courseService;

    public List<ProcedureResponse> getMyProcedures() {
        return procedureRepository.findByMemberIdOrderByProcedureDateDesc(Member.DEFAULT_ID).stream()
                .map(ProcedureResponse::from)
                .toList();
    }

    /** 지금 진행 중인 코스에 등록된 시술만 조회한다 - 코스가 끝나면 그 코스에서 등록한 시술은 더 이상 반환되지 않는다. */
    public List<ProcedureResponse> getProceduresForCourse(Long courseId) {
        return procedureRepository.findByMemberIdAndCourseIdOrderByProcedureDateDesc(Member.DEFAULT_ID, courseId).stream()
                .map(ProcedureResponse::from)
                .toList();
    }

    /**
     * GET /api/procedures/current용 - 진행 중인 코스가 아예 없으면 빈 목록을 반환한다. FOCUS 코스면
     * 그 코스에 귀속된 시술 전체를, DAILY 코스면 코스에 귀속된 시술이 없으므로 회원이 등록한 시술 중
     * 가장 최근 것 1개를 대신 반환한다.
     */
    public List<ProcedureResponse> getCurrentCourseProcedures() {
        Optional<CurrentCourseResponse> currentCourse = courseService.getCurrentCourse();
        if (currentCourse.isEmpty()) {
            return List.of();
        }

        CurrentCourseResponse course = currentCourse.get();
        if (course.courseType() == CourseType.FOCUS) {
            return getProceduresForCourse(course.courseId());
        }
        return getMyProcedures().stream().limit(1).toList();
    }

    public ProcedureResponse getProcedure(Long procedureId) {
        return ProcedureResponse.from(getOwnedProcedure(procedureId));
    }

    /** 시술은 항상 "지금 진행 중인 집중 코스"에 귀속시켜 등록한다 - 진행 중인 집중 코스가 없으면 등록할 수 없다. */
    @Transactional
    public List<ProcedureResponse> registerProcedures(ProcedureRegisterRequest request) {
        Long focusCourseId = getCurrentFocusCourseIdOrThrow();
        return request.procedures().stream()
                .map(item -> save(focusCourseId, item))
                .map(ProcedureResponse::from)
                .toList();
    }

    private Procedure save(Long courseId, ProcedureItemRequest item) {
        Procedure procedure = new Procedure(Member.DEFAULT_ID, courseId, item.procedureType(), item.procedureDate(), item.currentCount(), item.totalCount());
        item.areas().forEach(procedure::addArea);
        return procedureRepository.save(procedure);
    }

    private Long getCurrentFocusCourseIdOrThrow() {
        return courseService.getCurrentCourse()
                .filter(course -> course.courseType() == CourseType.FOCUS)
                .map(CurrentCourseResponse::courseId)
                .orElseThrow(() -> new CustomException(ProcedureErrorCode.FOCUS_COURSE_REQUIRED));
    }

    @Transactional
    public ProcedureResponse updateProcedure(Long procedureId, ProcedureItemRequest request) {
        Procedure procedure = getOwnedProcedure(procedureId);
        procedure.update(request.procedureType(), request.procedureDate(), request.currentCount(), request.totalCount(), request.areas());
        return ProcedureResponse.from(procedure);
    }

    @Transactional
    public void deleteProcedure(Long procedureId) {
        procedureRepository.delete(getOwnedProcedure(procedureId));
    }

    private Procedure getOwnedProcedure(Long procedureId) {
        return procedureRepository.findByIdAndMemberId(procedureId, Member.DEFAULT_ID)
                .orElseThrow(() -> new CustomException(ProcedureErrorCode.PROCEDURE_NOT_FOUND));
    }
}
