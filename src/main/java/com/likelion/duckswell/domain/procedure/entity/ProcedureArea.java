package com.likelion.duckswell.domain.procedure.entity;

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
@Table(name = "procedure_area")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcedureArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id", nullable = false)
    private Procedure procedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcedureAreaType area;

    ProcedureArea(Procedure procedure, ProcedureAreaType area) {
        this.procedure = procedure;
        this.area = area;
    }
}
