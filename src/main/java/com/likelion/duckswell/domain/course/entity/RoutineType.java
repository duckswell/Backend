package com.likelion.duckswell.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "routine_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineType {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoutineTypeCode code;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 500)
    private String iconUrl;

    public RoutineType(RoutineTypeCode code, String name, String description, String iconUrl) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
    }
}
