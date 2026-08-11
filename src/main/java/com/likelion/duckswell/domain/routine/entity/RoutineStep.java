package com.likelion.duckswell.domain.routine.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "routine_step")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false, length = 50)
    private String stepName;

    @Column(length = 150)
    private String productText;

    @Column(length = 300)
    private String methodText;

    @Column(length = 200)
    private String alternateText;

    @OneToMany(mappedBy = "routineStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineStepIngredient> ingredients = new ArrayList<>();

    RoutineStep(Routine routine, Integer stepOrder, String stepName, String productText, String methodText, String alternateText) {
        this.routine = routine;
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.productText = productText;
        this.methodText = methodText;
        this.alternateText = alternateText;
    }

    public void addIngredient(Long ingredientId, IngredientRole role) {
        ingredients.add(new RoutineStepIngredient(this, ingredientId, role));
    }
}
