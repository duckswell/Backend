package com.likelion.duckswell.domain.dashboard.repository;

import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByMemberIdAndCourseIdAndItemDateOrderByItemOrderAsc(Long memberId, Long courseId, LocalDate itemDate);

    Optional<ChecklistItem> findByIdAndMemberId(Long id, Long memberId);

    void deleteByMemberId(Long memberId);
}
