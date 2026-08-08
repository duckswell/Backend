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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "checklist_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate itemDate;

    @Column(nullable = false, length = 200)
    private String contentText;

    @Column(nullable = false)
    private boolean checked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChecklistSourceType sourceType;

    public ChecklistItem(Long memberId, LocalDate itemDate, String contentText, ChecklistSourceType sourceType) {
        this.memberId = memberId;
        this.itemDate = itemDate;
        this.contentText = contentText;
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
