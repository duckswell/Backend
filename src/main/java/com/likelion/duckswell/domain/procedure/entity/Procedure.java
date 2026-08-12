package com.likelion.duckswell.domain.procedure.entity;

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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "procedure_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Procedure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 시술은 등록 시점에 진행 중이던 집중 코스에 귀속된다 - 그 코스가 끝나면 이 시술도 더 이상 "현재 시술"로 취급되지 않는다. */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProcedureType procedureType;

    @Column(nullable = false)
    private LocalDate procedureDate;

    private Integer currentCount;

    private Integer totalCount;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcedureArea> areas = new ArrayList<>();

    public Procedure(Long memberId, Long courseId, ProcedureType procedureType, LocalDate procedureDate, Integer currentCount, Integer totalCount) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.procedureType = procedureType;
        this.procedureDate = procedureDate;
        this.currentCount = currentCount;
        this.totalCount = totalCount;
    }

    public void addArea(ProcedureAreaType areaType) {
        areas.add(new ProcedureArea(this, areaType));
    }

    public void updateCounts(Integer currentCount, Integer totalCount) {
        this.currentCount = currentCount;
        this.totalCount = totalCount;
    }

    public void update(ProcedureType procedureType, LocalDate procedureDate, Integer currentCount, Integer totalCount, List<ProcedureAreaType> areaTypes) {
        this.procedureType = procedureType;
        this.procedureDate = procedureDate;
        updateCounts(currentCount, totalCount);
        this.areas.clear();
        areaTypes.forEach(this::addArea);
    }
}
