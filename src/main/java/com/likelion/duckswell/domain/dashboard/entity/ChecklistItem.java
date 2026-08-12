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
                name = "uk_checklist_item_member_course_date_title",
                columnNames = {"member_id", "course_id", "item_date", "title"}
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
            String title,
            String description,
            ChecklistSourceType sourceType
    ) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.itemDate = itemDate;
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
