package com.likelion.duckswell.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CourseSymptomSummaryResponse;
import com.likelion.duckswell.domain.course.dto.IngredientCandidateResponse;
import com.likelion.duckswell.domain.course.dto.RecommendedProductResponse;
import com.likelion.duckswell.domain.course.dto.RoutineTypeIngredientResponse;
import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.entity.RoutineTypeIngredient;
import com.likelion.duckswell.domain.course.exception.CourseErrorCode;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeIngredientRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientCategory;
import com.likelion.duckswell.domain.product.entity.Product;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.product.repository.ProductRepository;
import com.likelion.duckswell.domain.product.service.ProductService;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import com.likelion.duckswell.global.exception.CustomException;
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
@Import({CourseService.class, ProductService.class})
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoutineTypeRepository routineTypeRepository;

    @Autowired
    private RoutineTypeIngredientRepository routineTypeIngredientRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 집중_코스를_시작하면_루틴타입_없이_진행중_상태로_생성된다() {
        // given
        endAnyActiveCourse();

        // when
        CourseResponse response = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // then
        assertThat(response.courseType()).isEqualTo(CourseType.FOCUS);
        assertThat(response.routineTypeCode()).isNull();
        assertThat(response.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void 집중_코스를_시작할_때_루틴타입을_같이_보내면_거부된다() {
        // given
        endAnyActiveCourse();

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, RoutineTypeCode.COOLDOWN)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ROUTINE_TYPE_NOT_ALLOWED_FOR_FOCUS);
    }

    @Test
    void 진행중인_코스가_있으면_새_코스를_시작할_수_없다() {
        // given
        endAnyActiveCourse();
        courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ACTIVE_COURSE_ALREADY_EXISTS);
    }

    @Test
    void 데일리_코스는_루틴타입을_지정하지_않으면_시작할_수_없다() {
        // given
        endAnyActiveCourse();

        // when & then
        assertThatThrownBy(() -> courseService.startCourse(new CourseStartRequest(CourseType.DAILY, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.ROUTINE_TYPE_REQUIRED_FOR_DAILY);
    }

    // ROUTINE_TYPE_NOT_FOUND(존재하지 않는 루틴타입) 분기는 이 테스트 스위트에서 자동으로 검증하지 못한다:
    // routine_type은 4개 코드가 항상 전부 시드되어 있고 여러 테이블이 FK로 물려 있어,
    // 특정 코드를 안전하게 "없는 상태"로 만들 방법이 없다(Mockito 등 모킹 의존성도 없음).
    // startCourse/analyze.py 수동 curl 검증(2026-08-10) 때 시드 전 상태에서 CO006으로 정상 동작 확인함.

    @Test
    void 데일리_코스를_시작하면_지정한_루틴타입이_반영된다() {
        // given
        endAnyActiveCourse();
        seedRoutineType(RoutineTypeCode.HYDRATION);

        // when
        CourseResponse response = courseService.startCourse(new CourseStartRequest(CourseType.DAILY, RoutineTypeCode.HYDRATION));

        // then
        assertThat(response.routineTypeCode()).isEqualTo(RoutineTypeCode.HYDRATION);
        assertThat(response.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void 진행중인_코스를_종료하면_completed_상태가_된다() {
        // given
        endAnyActiveCourse();
        CourseResponse started = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when
        CourseResponse ended = courseService.endCourse(started.id());

        // then
        assertThat(ended.status()).isEqualTo(CourseStatus.COMPLETED);
        assertThat(ended.endedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void 이미_종료된_코스는_다시_종료할_수_없다() {
        // given
        endAnyActiveCourse();
        CourseResponse started = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));
        courseService.endCourse(started.id());

        // when & then
        assertThatThrownBy(() -> courseService.endCourse(started.id()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.COURSE_ALREADY_ENDED);
    }

    @Test
    void 집중_코스로_돌아가면_기존_진행중_코스는_종료되고_새_집중_코스가_생긴다() {
        // given
        endAnyActiveCourse();
        seedRoutineType(RoutineTypeCode.COOLDOWN);
        CourseResponse daily = courseService.startCourse(new CourseStartRequest(CourseType.DAILY, RoutineTypeCode.COOLDOWN));

        // when
        CourseResponse restarted = courseService.restartFocusCourse();

        // then
        assertThat(restarted.courseType()).isEqualTo(CourseType.FOCUS);
        assertThat(restarted.routineTypeCode()).isNull();
        Course reloadedDaily = courseRepository.findById(daily.id()).orElseThrow();
        assertThat(reloadedDaily.getStatus()).isEqualTo(CourseStatus.COMPLETED);
    }

    @Test
    void 코스_기록_조회에_종료된_코스와_진행중인_코스가_모두_포함된다() {
        // given
        endAnyActiveCourse();
        CourseResponse first = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));
        courseService.endCourse(first.id());
        CourseResponse second = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when
        List<CourseResponse> history = courseService.getCourseHistory();

        // then: 같은 날 생성돼 startedAt이 동일할 수 있어 순서(index)가 아닌 id 기준으로 상태를 확인한다
        assertThat(history).extracting(CourseResponse::id).contains(first.id(), second.id());
        assertThat(history).filteredOn(c -> c.id().equals(first.id()))
                .extracting(CourseResponse::status)
                .containsExactly(CourseStatus.COMPLETED);
        assertThat(history).filteredOn(c -> c.id().equals(second.id()))
                .extracting(CourseResponse::status)
                .containsExactly(CourseStatus.IN_PROGRESS);
    }

    @Test
    void 관리_타입에_배정된_성분이_후보군에_포함된다() {
        // given
        seedRoutineType(RoutineTypeCode.HYDRATION);
        Ingredient ingredient = ingredientRepository.save(new Ingredient("히알루론산", IngredientCategory.MOISTURE, "보습 성분"));
        RoutineType hydration = routineTypeRepository.findById(RoutineTypeCode.HYDRATION).orElseThrow();
        routineTypeIngredientRepository.save(new RoutineTypeIngredient(hydration, ingredient.getId()));
        entityManager.flush();

        // when
        List<IngredientCandidateResponse> candidates = courseService.getIngredientCandidates(RoutineTypeCode.HYDRATION);

        // then
        assertThat(candidates).extracting(IngredientCandidateResponse::ingredientId).contains(ingredient.getId());
    }

    @Test
    void 루틴_타입별_성분_목록_조회시_ingredientId와_ingredientName이_함께_반환된다() {
        // given
        seedRoutineType(RoutineTypeCode.CLEAR_UP);
        Ingredient ingredient = ingredientRepository.save(new Ingredient("나이아신아마이드", IngredientCategory.VITAMIN, "미백 성분"));
        RoutineType clearUp = routineTypeRepository.findById(RoutineTypeCode.CLEAR_UP).orElseThrow();
        routineTypeIngredientRepository.save(new RoutineTypeIngredient(clearUp, ingredient.getId()));
        entityManager.flush();

        // when
        List<RoutineTypeIngredientResponse> response = courseService.getRoutineTypeIngredients(RoutineTypeCode.CLEAR_UP);

        // then
        assertThat(response).extracting(RoutineTypeIngredientResponse::ingredientId, RoutineTypeIngredientResponse::ingredientName)
                .contains(tuple(ingredient.getId(), ingredient.getName()));
    }

    @Test
    void routineTypeCode가_없으면_새로_추가한_성분도_포함된_전체_목록이_반환된다() {
        // given
        Ingredient ingredient = ingredientRepository.save(new Ingredient("나이아신아마이드", IngredientCategory.VITAMIN, "미백 성분"));
        entityManager.flush();

        // when
        List<IngredientCandidateResponse> candidates = courseService.getIngredientCandidates(null);

        // then
        assertThat(candidates).extracting(IngredientCandidateResponse::ingredientId).contains(ingredient.getId());
    }

    @Test
    void 최근_7일_증상만_집계되고_그_이전_증상은_제외된다() {
        // given
        endAnyActiveCourse();
        Course course = courseRepository.save(new Course(Member.DEFAULT_ID, null, CourseType.FOCUS, null, LocalDate.now()));

        Routine withinWindow = new Routine(course.getId(), LocalDate.now(), null, null);
        withinWindow.addSymptom(Symptom.DRYNESS);
        withinWindow.addSymptom(Symptom.DRYNESS);
        withinWindow.addSymptom(Symptom.FLAKING);
        routineRepository.save(withinWindow);

        Routine outsideWindow = new Routine(course.getId(), LocalDate.now().minusDays(10), null, null);
        outsideWindow.addSymptom(Symptom.HEAT);
        routineRepository.save(outsideWindow);

        entityManager.flush();

        // when
        CourseSymptomSummaryResponse summary = courseService.getSymptomSummary(course.getId());

        // then: DRYNESS 2회가 1위 키워드, 10일 전 HEAT는 집계에서 제외되고
        // top2(DRYNESS→{HYDRATION}, FLAKING→{HYDRATION,CLEAR_UP})의 교집합은 HYDRATION 하나뿐이라
        // 추천 타입도 HYDRATION이 된다 (day-after-detailed-flow-spec.md 교집합 알고리즘)
        assertThat(summary.topSymptoms()).extracting(CourseSymptomSummaryResponse.SymptomFrequency::symptom)
                .doesNotContain(Symptom.HEAT);
        assertThat(summary.topSymptoms().get(0).symptom()).isEqualTo(Symptom.DRYNESS);
        assertThat(summary.topSymptoms().get(0).count()).isEqualTo(2);
        assertThat(summary.recommendedRoutineTypeCode()).isEqualTo(RoutineTypeCode.HYDRATION);
    }

    @Test
    void 교집합이_없는_두_증상이_top2면_클리어업이_기본값으로_추천된다() {
        // given
        endAnyActiveCourse();
        Course course = courseRepository.save(new Course(Member.DEFAULT_ID, null, CourseType.FOCUS, null, LocalDate.now()));

        // DRYNESS -> {HYDRATION}, OILINESS -> {SEBUM_CONTROL}: 교집합 없음 -> 기본값 CLEAR_UP
        Routine routine = new Routine(course.getId(), LocalDate.now(), null, null);
        routine.addSymptom(Symptom.DRYNESS);
        routine.addSymptom(Symptom.OILINESS);
        routineRepository.save(routine);
        entityManager.flush();

        // when
        CourseSymptomSummaryResponse summary = courseService.getSymptomSummary(course.getId());

        // then
        assertThat(summary.recommendedRoutineTypeCode()).isEqualTo(RoutineTypeCode.CLEAR_UP);
    }

    @Test
    void 최근_증상_기록이_없으면_요약과_추천_필드가_모두_비어있다() {
        // given
        endAnyActiveCourse();
        CourseResponse started = courseService.startCourse(new CourseStartRequest(CourseType.FOCUS, null));

        // when
        CourseSymptomSummaryResponse summary = courseService.getSymptomSummary(started.id());

        // then
        assertThat(summary.topSymptoms()).isEmpty();
        assertThat(summary.recommendedRoutineTypeCode()).isNull();
        assertThat(summary.recommendedRoutineTypeName()).isNull();
    }

    @Test
    void 성분마다_제품을_하나씩_상한없이_뽑는다() {
        // given: 후보 4개 - 예전 3개 상한이 남아있었다면 이 테스트가 실패해야 진짜로 "상한 없음"이 검증된다
        Ingredient niacinamide = ingredientRepository.save(new Ingredient("나이아신아마이드", IngredientCategory.VITAMIN, "미백 성분"));
        Ingredient panthenol = ingredientRepository.save(new Ingredient("판테놀", IngredientCategory.MOISTURE, "보습 성분"));
        Ingredient centella = ingredientRepository.save(new Ingredient("센텔라", IngredientCategory.PLANT_EXTRACT, "진정 성분"));
        Ingredient ceramide = ingredientRepository.save(new Ingredient("세라마이드", IngredientCategory.MOISTURE, "장벽 성분"));
        Product niacinamideSerum = productRepository.save(
                new Product(niacinamide, "나이아신아마이드 세럼", "브랜드A", ProductCategory.AMPOULE_SERUM, null, null));
        Product panthenolCream = productRepository.save(
                new Product(panthenol, "판테놀 크림", "브랜드B", ProductCategory.CREAM, null, null));
        Product centellaCream = productRepository.save(
                new Product(centella, "센텔라 크림", "브랜드C", ProductCategory.CREAM, null, null));
        Product ceramideCream = productRepository.save(
                new Product(ceramide, "세라마이드 크림", "브랜드D", ProductCategory.CREAM, null, null));
        entityManager.flush();

        // when
        List<RecommendedProductResponse> recommended = courseService.pickRecommendedProducts(List.of(
                new CourseService.ProductPickCandidate(niacinamide.getId(), null),
                new CourseService.ProductPickCandidate(panthenol.getId(), null),
                new CourseService.ProductPickCandidate(centella.getId(), null),
                new CourseService.ProductPickCandidate(ceramide.getId(), null)));

        // then
        assertThat(recommended).extracting(r -> r.product().id())
                .containsExactlyInAnyOrder(
                        niacinamideSerum.getId(), panthenolCream.getId(), centellaCream.getId(), ceramideCream.getId());
    }

    private void seedRoutineType(RoutineTypeCode code) {
        if (routineTypeRepository.existsById(code)) {
            return;
        }
        routineTypeRepository.save(new RoutineType(code, code.name(), null, null));
    }

    /** 시드 데이터 등으로 이미 진행 중인 코스가 있으면 종료해서, 각 테스트가 "진행 중인 코스 없음" 상태에서 시작하도록 만든다. */
    private void endAnyActiveCourse() {
        courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .ifPresent(course -> course.end(LocalDate.now()));
        entityManager.flush();
    }
}
