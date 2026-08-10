package com.likelion.duckswell.domain.procedure.repository;

import com.likelion.duckswell.domain.procedure.entity.Procedure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureRepository extends JpaRepository<Procedure, Long> {

    List<Procedure> findByMemberIdOrderByProcedureDateDesc(Long memberId);

    Optional<Procedure> findByIdAndMemberId(Long id, Long memberId);
}
