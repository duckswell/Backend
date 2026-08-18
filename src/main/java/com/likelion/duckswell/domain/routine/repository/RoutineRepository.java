package com.likelion.duckswell.domain.routine.repository;

import com.likelion.duckswell.domain.routine.entity.Routine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findByCourseIdOrderByRoutineDateDesc(Long courseId);

    /** 같은 날짜에 여러 번 기록될 수 있어서, 그 중 가장 최근 것 하나를 가져온다. */
    Optional<Routine> findFirstByCourseIdAndRoutineDateOrderByCreatedAtDesc(Long courseId, LocalDate routineDate);
}
