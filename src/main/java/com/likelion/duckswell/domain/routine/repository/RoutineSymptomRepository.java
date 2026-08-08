package com.likelion.duckswell.domain.routine.repository;

import com.likelion.duckswell.domain.routine.entity.RoutineSymptom;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineSymptomRepository extends JpaRepository<RoutineSymptom, Long> {

    @Query("select rs.symptom from RoutineSymptom rs "
            + "where rs.routine.courseId = :courseId and rs.routine.routineDate >= :fromDate")
    List<Symptom> findSymptomsByCourseIdAndDateFrom(@Param("courseId") Long courseId, @Param("fromDate") LocalDate fromDate);
}
