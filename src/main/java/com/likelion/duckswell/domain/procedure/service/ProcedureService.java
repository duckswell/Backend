package com.likelion.duckswell.domain.procedure.service;

import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.procedure.dto.ProcedureItemRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.entity.Procedure;
import com.likelion.duckswell.domain.procedure.repository.ProcedureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcedureService {

    private final ProcedureRepository procedureRepository;

    public List<ProcedureResponse> getMyProcedures() {
        return procedureRepository.findByMemberIdOrderByProcedureDateDesc(Member.DEFAULT_ID).stream()
                .map(ProcedureResponse::from)
                .toList();
    }

    @Transactional
    public List<ProcedureResponse> registerProcedures(ProcedureRegisterRequest request) {
        return request.procedures().stream()
                .map(this::save)
                .map(ProcedureResponse::from)
                .toList();
    }

    private Procedure save(ProcedureItemRequest item) {
        Procedure procedure = new Procedure(Member.DEFAULT_ID, item.procedureType(), item.procedureDate(), item.currentCount(), item.totalCount());
        item.areas().forEach(procedure::addArea);
        return procedureRepository.save(procedure);
    }
}
