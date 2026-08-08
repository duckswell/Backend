package com.likelion.duckswell.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "routine_type_procedure_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineTypeProcedureRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_type_code", nullable = false)
    private RoutineType routineType;

    @Column(nullable = false, length = 100)
    private String procedureName;

    @Column(length = 200)
    private String description;

    public RoutineTypeProcedureRecommendation(RoutineType routineType, String procedureName, String description) {
        this.routineType = routineType;
        this.procedureName = procedureName;
        this.description = description;
    }
}
