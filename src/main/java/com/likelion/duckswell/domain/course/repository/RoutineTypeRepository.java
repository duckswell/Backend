package com.likelion.duckswell.domain.course.repository;

import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineTypeRepository extends JpaRepository<RoutineType, RoutineTypeCode> {
}
