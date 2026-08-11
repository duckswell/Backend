package com.likelion.duckswell.domain.routine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.duckswell.domain.course.dto.RecommendedProductResponse;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientCategory;
import com.likelion.duckswell.domain.product.entity.Product;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.product.repository.ProductRepository;
import com.likelion.duckswell.domain.product.service.ProductService;
import com.likelion.duckswell.domain.routine.dto.RoutineStepSummaryResponse;
import com.likelion.duckswell.domain.routine.entity.IngredientRole;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineStep;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RoutineService.class, CourseService.class, ProductService.class})
class RoutineServiceTest {

    private static final Long DUMMY_COURSE_ID = -9999L;

    @Autowired
    private RoutineService routineService;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 스텝_요약이_대표_성분_id_이름과_카테고리를_반환한다() {
        // given
        Ingredient ingredient = ingredientRepository.save(new Ingredient("센텔라", IngredientCategory.PLANT_EXTRACT, "진정 성분"));
        Routine routine = new Routine(DUMMY_COURSE_ID, LocalDate.now(), null, null);
        RoutineStep step = routine.addStep(1, "진정 크림", ProductCategory.CREAM, null, null, null);
        step.addIngredient(ingredient.getId(), IngredientRole.PRIMARY);
        routineRepository.save(routine);
        entityManager.flush();

        // when
        List<RoutineStepSummaryResponse> summaries = routineService.getStepSummaries(routine.getId());

        // then
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).category()).isEqualTo(ProductCategory.CREAM);
        assertThat(summaries.get(0).ingredientId()).isEqualTo(ingredient.getId());
        assertThat(summaries.get(0).ingredientName()).isEqualTo("센텔라");
    }

    @Test
    void 클렌징_스텝은_성분_id와_이름이_모두_null이다() {
        // given
        Routine routine = new Routine(DUMMY_COURSE_ID, LocalDate.now(), null, null);
        routine.addStep(1, "클렌징", ProductCategory.CLEANSER, null, null, null);
        routineRepository.save(routine);
        entityManager.flush();

        // when
        List<RoutineStepSummaryResponse> summaries = routineService.getStepSummaries(routine.getId());

        // then
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).ingredientId()).isNull();
        assertThat(summaries.get(0).ingredientName()).isNull();
    }

    @Test
    void 루틴_완료_기준_추천제품이_클렌저를_제외하고_성분당_1개씩_상한없이_뽑힌다() {
        // given: 활성 스텝 4개 - 예전 3개 상한이 남아있었다면 이 테스트가 실패해야 진짜로 "상한 없음"이 검증된다
        Ingredient centella = ingredientRepository.save(new Ingredient("센텔라", IngredientCategory.PLANT_EXTRACT, "진정 성분"));
        Ingredient panthenol = ingredientRepository.save(new Ingredient("판테놀", IngredientCategory.MOISTURE, "보습 성분"));
        Ingredient hyaluronic = ingredientRepository.save(new Ingredient("히알루론산", IngredientCategory.MOISTURE, "수분 성분"));
        Ingredient niacinamide = ingredientRepository.save(new Ingredient("나이아신아마이드", IngredientCategory.VITAMIN, "미백 성분"));
        Product cream = productRepository.save(new Product(centella, "센텔라 크림", "브랜드A", ProductCategory.CREAM, null, null));
        Product serum = productRepository.save(new Product(panthenol, "판테놀 세럼", "브랜드B", ProductCategory.AMPOULE_SERUM, null, null));
        Product toner = productRepository.save(new Product(hyaluronic, "히알루론산 토너", "브랜드C", ProductCategory.SKIN_TONER, null, null));
        Product mist = productRepository.save(new Product(niacinamide, "나이아신아마이드 미스트", "브랜드D", ProductCategory.MIST_OIL, null, null));

        Routine routine = new Routine(DUMMY_COURSE_ID, LocalDate.now(), null, null);
        routine.addStep(1, "클렌징", ProductCategory.CLEANSER, null, null, null); // 성분 없음 - 결과에 영향 없어야 함
        RoutineStep creamStep = routine.addStep(2, "진정 크림", ProductCategory.CREAM, null, null, null);
        creamStep.addIngredient(centella.getId(), IngredientRole.PRIMARY);
        RoutineStep serumStep = routine.addStep(3, "보습 세럼", ProductCategory.AMPOULE_SERUM, null, null, null);
        serumStep.addIngredient(panthenol.getId(), IngredientRole.PRIMARY);
        RoutineStep tonerStep = routine.addStep(4, "수분 토너", ProductCategory.SKIN_TONER, null, null, null);
        tonerStep.addIngredient(hyaluronic.getId(), IngredientRole.PRIMARY);
        RoutineStep mistStep = routine.addStep(5, "미백 미스트", ProductCategory.MIST_OIL, null, null, null);
        mistStep.addIngredient(niacinamide.getId(), IngredientRole.PRIMARY);
        routineRepository.save(routine);
        entityManager.flush();

        // when
        List<RecommendedProductResponse> recommended = routineService.getRecommendedProducts(routine.getId());

        // then: 클렌징 스텝은 성분이 없어 자연히 빠지고, 나머지 네 성분 각각 제품 1개씩(3개 상한 없음)
        assertThat(recommended).hasSize(4);
        assertThat(recommended).extracting(r -> r.product().id())
                .containsExactlyInAnyOrder(cream.getId(), serum.getId(), toner.getId(), mist.getId());
        assertThat(recommended).extracting(RecommendedProductResponse::ingredientName)
                .containsExactlyInAnyOrder("센텔라", "판테놀", "히알루론산", "나이아신아마이드");
    }
}
