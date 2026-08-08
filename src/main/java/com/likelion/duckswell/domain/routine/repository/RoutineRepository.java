package com.likelion.duckswell.domain.routine.repository;

import com.likelion.duckswell.domain.routine.entity.Routine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findByCourseIdOrderByRoutineDateDesc(Long courseId);

    Optional<Routine> findByCourseIdAndRoutineDate(Long courseId, LocalDate routineDate);
}
