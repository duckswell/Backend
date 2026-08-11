package com.likelion.duckswell.domain.routine.entity;

import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "routine")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDate routineDate;

    private String photoUrl;

    @Column(length = 500)
    private String symptomNote;

    @Enumerated(EnumType.STRING)
    private RoutineDifficulty difficulty;

    private Integer estimatedMinutes;

    @Column(length = 500)
    private String reasonText;

    @Column(length = 500)
    private String completionSummaryText;

    @Column(length = 500)
    private String recoveryStageSummaryText;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineSymptom> symptoms = new ArrayList<>();

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineStep> steps = new ArrayList<>();

    public Routine(Long courseId, LocalDate routineDate, String photoUrl, String symptomNote) {
        this.courseId = courseId;
        this.routineDate = routineDate;
        this.photoUrl = photoUrl;
        this.symptomNote = symptomNote;
    }

    public void addSymptom(Symptom symptom) {
        symptoms.add(new RoutineSymptom(this, symptom));
    }

    public void selectDifficulty(RoutineDifficulty difficulty, Integer estimatedMinutes, String reasonText) {
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.reasonText = reasonText;
    }

    public RoutineStep addStep(int order, String stepName, ProductCategory category,
            String productText, String methodText, String alternateText) {
        RoutineStep step = new RoutineStep(this, order, stepName, category, productText, methodText, alternateText);
        steps.add(step);
        return step;
    }

    /** 난이도를 다시 선택할 때 이전 생성 결과를 지우는 용도 - orphanRemoval로 기존 스텝 row도 함께 삭제된다. */
    public void resetSteps() {
        steps.clear();
    }

    public void complete(String completionSummaryText) {
        this.completionSummaryText = completionSummaryText;
        this.completedAt = LocalDateTime.now();
    }

    public void cacheRecoveryStageSummary(String recoveryStageSummaryText) {
        this.recoveryStageSummaryText = recoveryStageSummaryText;
    }
}
