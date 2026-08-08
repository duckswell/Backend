package com.likelion.duckswell.domain.procedure.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, length = 50)
    private String procedureType;

    @Column(nullable = false)
    private LocalDate procedureDate;

    private Integer currentCount;

    private Integer totalCount;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcedureArea> areas = new ArrayList<>();

    public Procedure(Long memberId, String procedureType, LocalDate procedureDate, Integer currentCount, Integer totalCount) {
        this.memberId = memberId;
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
}
