package com.likelion.duckswell.domain.course.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "procedure_id")
    private Long procedureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CourseType courseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_type_code")
    private RoutineType routineType;

    @Column(nullable = false)
    private LocalDate startedAt;

    private LocalDate endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    public Course(Long memberId, Long procedureId, CourseType courseType, RoutineType routineType, LocalDate startedAt) {
        this.memberId = memberId;
        this.procedureId = procedureId;
        this.courseType = courseType;
        this.routineType = routineType;
        this.startedAt = startedAt;
        this.status = CourseStatus.IN_PROGRESS;
    }

    public void end(LocalDate endedAt) {
        this.endedAt = endedAt;
        this.status = CourseStatus.COMPLETED;
    }

    public void changeRoutineType(RoutineType routineType) {
        this.routineType = routineType;
    }
}
