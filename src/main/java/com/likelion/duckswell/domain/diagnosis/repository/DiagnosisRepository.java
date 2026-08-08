package com.likelion.duckswell.domain.diagnosis.repository;

import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByRoutineId(Long routineId);
}
