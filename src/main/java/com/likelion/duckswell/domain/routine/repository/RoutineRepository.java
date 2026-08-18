package com.likelion.duckswell.domain.routine.repository;

import com.likelion.duckswell.domain.routine.entity.Routine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findByCourseIdOrderByRoutineDateDesc(Long courseId);

    /**
     * routineDate가 같으면 id(생성 순서)로 한 번 더 정렬한다 - 같은 날 여러 번 기록해도 순서가
     * 뒤섞이지 않게. courseIds를 여러 개 넘기면 코스 경계와 무관하게(코스가 새로 시작돼도 끊기지
     * 않고) 회원의 전체 기록을 한 흐름으로 최신순 조회할 수 있다.
     */
    List<Routine> findByCourseIdInOrderByRoutineDateDescIdDesc(List<Long> courseIds);

    /** 같은 날짜에 여러 번 기록될 수 있어서, 그 중 가장 최근 것 하나를 가져온다. */
    Optional<Routine> findFirstByCourseIdAndRoutineDateOrderByCreatedAtDesc(Long courseId, LocalDate routineDate);
}
