package com.likelion.duckswell.domain.routine.repository;

import com.likelion.duckswell.domain.routine.entity.RoutineStepIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineStepIngredientRepository extends JpaRepository<RoutineStepIngredient, Long> {

    @Query("select rsi.ingredientId from RoutineStepIngredient rsi "
            + "where rsi.routineStep.routine.id in :routineIds "
            + "group by rsi.ingredientId "
            + "order by count(rsi) desc, max(rsi.routineStep.routine.routineDate) desc, rsi.ingredientId asc")
    List<Long> findIngredientIdsOrderByFrequencyDesc(@Param("routineIds") List<Long> routineIds);
}
