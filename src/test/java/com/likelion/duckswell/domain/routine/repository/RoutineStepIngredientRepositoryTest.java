package com.likelion.duckswell.domain.routine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.routine.entity.IngredientRole;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineStep;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoutineStepIngredientRepositoryTest {

    private static final Long DUMMY_COURSE_ID = -9999L;
    private static final Long INGREDIENT_A = -9001L;
    private static final Long INGREDIENT_B = -9002L;
    private static final Long INGREDIENT_C = -9003L;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private RoutineStepIngredientRepository routineStepIngredientRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 빈도가_같으면_더_최근에_추천된_성분이_먼저_정렬된다() {
        // given: C는 2회 추천되어 1순위, A와 B는 1회씩으로 동점이지만 B가 더 최근 루틴에서 추천됨
        Routine routine1 = new Routine(DUMMY_COURSE_ID, LocalDate.now().minusDays(3), null, null);
        routine1.addStep(1, "스텝1", ProductCategory.CREAM, null, null, null).addIngredient(INGREDIENT_A, IngredientRole.PRIMARY);
        routineRepository.save(routine1);

        Routine routine2 = new Routine(DUMMY_COURSE_ID, LocalDate.now().minusDays(2), null, null);
        routine2.addStep(1, "스텝1", ProductCategory.CREAM, null, null, null).addIngredient(INGREDIENT_C, IngredientRole.PRIMARY);
        routineRepository.save(routine2);

        Routine routine3 = new Routine(DUMMY_COURSE_ID, LocalDate.now().minusDays(1), null, null);
        RoutineStep step3 = routine3.addStep(1, "스텝1", ProductCategory.CREAM, null, null, null);
        step3.addIngredient(INGREDIENT_C, IngredientRole.PRIMARY);
        routine3.addStep(2, "스텝2", ProductCategory.CREAM, null, null, null).addIngredient(INGREDIENT_B, IngredientRole.PRIMARY);
        routineRepository.save(routine3);

        entityManager.flush();

        // when
        List<Long> routineIds = List.of(routine1.getId(), routine2.getId(), routine3.getId());
        List<Long> result = routineStepIngredientRepository.findIngredientIdsOrderByFrequencyDesc(routineIds);

        // then: C(2회) > B(1회, 더 최근) > A(1회, 더 오래됨)
        assertThat(result).containsExactly(INGREDIENT_C, INGREDIENT_B, INGREDIENT_A);
    }
}
