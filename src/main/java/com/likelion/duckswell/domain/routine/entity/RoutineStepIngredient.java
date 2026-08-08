package com.likelion.duckswell.domain.routine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "routine_step_ingredient")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineStepIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_step_id", nullable = false)
    private RoutineStep routineStep;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingredient_role", nullable = false, length = 10)
    private IngredientRole ingredientRole;

    RoutineStepIngredient(RoutineStep routineStep, Long ingredientId, IngredientRole ingredientRole) {
        this.routineStep = routineStep;
        this.ingredientId = ingredientId;
        this.ingredientRole = ingredientRole;
    }
}
