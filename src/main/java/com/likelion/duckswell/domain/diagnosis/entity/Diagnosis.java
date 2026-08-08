package com.likelion.duckswell.domain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "diagnosis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routine_id", nullable = false, unique = true)
    private Long routineId;

    private Integer rednessScore;

    private Integer textureScore;

    private Integer blemishScore;

    @Column(length = 500)
    private String summaryText;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    public Diagnosis(Long routineId, Integer rednessScore, Integer textureScore, Integer blemishScore, String summaryText) {
        this.routineId = routineId;
        this.rednessScore = rednessScore;
        this.textureScore = textureScore;
        this.blemishScore = blemishScore;
        this.summaryText = summaryText;
        this.analyzedAt = LocalDateTime.now();
    }
}
