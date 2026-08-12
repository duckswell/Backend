package com.likelion.duckswell.domain.dashboard.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "checklist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checklist_item_member_course_date_order",
                columnNames = {"member_id", "course_id", "item_date", "item_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDate itemDate;

    /** 하루 생성 배치 내 순번(0, 1) - LLM 응답 문구와 무관하게 (memberId, courseId, itemDate) 당 단일 생성을 DB 레벨에서 보장하기 위한 키. */
    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private boolean checked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChecklistSourceType sourceType;

    public ChecklistItem(
            Long memberId,
            Long courseId,
            LocalDate itemDate,
            int itemOrder,
            String title,
            String description,
            ChecklistSourceType sourceType
    ) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.itemDate = itemDate;
        this.itemOrder = itemOrder;
        this.title = title;
        this.description = description;
        this.sourceType = sourceType;
        this.checked = false;
    }

    public void check() {
        this.checked = true;
    }

    public void uncheck() {
        this.checked = false;
    }
}
