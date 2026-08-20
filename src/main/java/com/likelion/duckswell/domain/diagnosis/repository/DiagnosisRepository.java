package com.likelion.duckswell.domain.diagnosis.repository;

import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisScoreSnapshot;
import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByRoutineId(Long routineId);

    void deleteByRoutineIdIn(List<Long> routineIds);

    /**
     * courseIds에 속한 루틴 중 점수 3개가 모두 채워진 진단만 골라, 루틴 날짜(같으면 진단 기록
     * 자체의 id) 내림차순으로 DB에서 직접 pageable 크기만큼 제한 조회한다 - 전체 이력을 메모리에
     * 올려 애플리케이션에서 필터링/정렬/제한하지 않기 위함. 같은 날짜에 여러 기록이 있을 때
     * routine id가 아니라 diagnosis id로 정렬해야, 루틴 생성 순서가 아니라 실제 진단이 기록된
     * 순서로 결정적으로 정렬된다. 필요한 점수 3개만 프로젝션한다(회복 배너의 "현재/직전 기록"
     * 비교에 사용).
     */
    @Query("""
            SELECT new com.likelion.duckswell.domain.diagnosis.dto.DiagnosisScoreSnapshot(d.rednessScore, d.textureScore, d.blemishScore)
            FROM Diagnosis d JOIN Routine r ON d.routineId = r.id
            WHERE r.courseId IN :courseIds
              AND d.rednessScore IS NOT NULL
              AND d.textureScore IS NOT NULL
              AND d.blemishScore IS NOT NULL
            ORDER BY r.routineDate DESC, d.id DESC
            """)
    List<DiagnosisScoreSnapshot> findRecentValidScores(@Param("courseIds") List<Long> courseIds, Pageable pageable);
}
