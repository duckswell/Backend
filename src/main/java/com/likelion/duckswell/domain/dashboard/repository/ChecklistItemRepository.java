package com.likelion.duckswell.domain.dashboard.repository;

import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByMemberIdAndItemDate(Long memberId, LocalDate itemDate);
}
