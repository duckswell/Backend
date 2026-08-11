package com.likelion.duckswell.domain.course.repository;

import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.entity.RoutineTypeIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineTypeIngredientRepository extends JpaRepository<RoutineTypeIngredient, Long> {

    List<RoutineTypeIngredient> findByRoutineType_Code(RoutineTypeCode code);
}
