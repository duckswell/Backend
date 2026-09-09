package com.likelion.duckswell.domain.course.service;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CourseSymptomSummaryResponse;
import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
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
import com.likelion.duckswell.domain.course.util.SymptomRoutineTypeMapper;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.product.dto.ProductResponse;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientTag;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.product.repository.IngredientTagRepository;
import com.likelion.duckswell.domain.product.service.ProductService;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import com.likelion.duckswell.domain.routine.repository.RoutineSymptomRepository;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final int SYMPTOM_LOOKBACK_DAYS = 6;

    private final CourseRepository courseRepository;
    private final RoutineTypeRepository routineTypeRepository;
    private final RoutineRepository routineRepository;
    private final RoutineTypeIngredientRepository routineTypeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientTagRepository ingredientTagRepository;
    private final RoutineSymptomRepository routineSymptomRepository;
    private final ProductService productService;

    /** category가 null이면 해당 성분의 카테고리 제한 없이 조회한다. RoutineService도 이 타입으로 후보를 넘긴다. */
    public record ProductPickCandidate(Long ingredientId, ProductCategory category) {
    }

    @Transactional
    public CourseResponse startCourse(CourseStartRequest request) {
        courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .ifPresent(course -> {
                    throw new CustomException(CourseErrorCode.ACTIVE_COURSE_ALREADY_EXISTS);
                });

        RoutineType routineType = resolveRoutineTypeForStart(request.courseType(), request.routineTypeCode());

        Course course = new Course(Member.DEFAULT_ID, null, request.courseType(), routineType, LocalDate.now());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse endCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        if (course.getStatus() != CourseStatus.IN_PROGRESS) {
            throw new CustomException(CourseErrorCode.COURSE_ALREADY_ENDED);
        }
        course.end(LocalDate.now());
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse restartFocusCourse() {
        Optional<Course> activeCourse = courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS);
        activeCourse.ifPresent(course -> course.end(LocalDate.now()));

        Course newCourse = new Course(Member.DEFAULT_ID, null, CourseType.FOCUS, null, LocalDate.now());
        return CourseResponse.from(courseRepository.save(newCourse));
    }

    public List<CourseResponse> getCourseHistory() {
        return courseRepository.findByMemberIdOrderByStartedAtDescIdDesc(Member.DEFAULT_ID).stream()
                .map(CourseResponse::from)
                .toList();
    }

    /** 다른 도메인(diagnosis 등)이 courseId만으로 코스 정보를 참조해야 할 때 쓰는 조회용 메서드. */
    public CourseResponse getCourse(Long courseId) {
        return CourseResponse.from(getCourseOrThrow(courseId));
    }

    /** routineTypeCode가 null(FOCUS 코스)이면 확정 8개 성분 전체를 후보군으로 fallback한다. */
    public List<IngredientCandidateResponse> getIngredientCandidates(RoutineTypeCode routineTypeCode) {
        List<Ingredient> ingredients = routineTypeCode != null
                ? ingredientRepository.findAllById(
                        routineTypeIngredientRepository.findByRoutineType_Code(routineTypeCode).stream()
                                .map(RoutineTypeIngredient::getIngredientId)
                                .toList())
                : ingredientRepository.findAll();

        return ingredients.stream()
                .map(ingredient -> new IngredientCandidateResponse(
                        ingredient.getId(),
                        ingredient.getName(),
                        ingredientTagRepository.findByIngredientId(ingredient.getId()).stream()
                                .map(IngredientTag::getTag)
                                .toList()
                ))
                .toList();
    }

    /**
     * 아직 오늘의 Routine이 없는 시점(데일리 루틴 타입을 방금 선택한 직후)에, 그 루틴 타입의 고정
     * 후보 성분 전체를 기준으로 추천 제품을 조회한다. 스텝별 카테고리가 아직 없으므로 카테고리
     * 제한 없이(null) 성분당 제품 하나씩 뽑는다.
     */
    public List<RecommendedProductResponse> getRecommendedProductsByRoutineType(RoutineTypeCode routineTypeCode) {
        List<ProductPickCandidate> candidates = routineTypeIngredientRepository.findByRoutineType_Code(routineTypeCode).stream()
                .map(ingredient -> new ProductPickCandidate(ingredient.getIngredientId(), null))
                .toList();
        return pickRecommendedProducts(candidates);
    }

    /**
     * 아직 오늘의 Routine이 없는 시점에도 그 루틴 타입에 고정 매핑된 성분 전체를 ingredientId와
     * 함께 보여주기 위한 조회. 더보기에서 성분 카드를 나열하고, 카드 클릭 시 그 ingredientId로
     * 제품 목록 API를 다시 호출하는 화면에 쓰인다.
     */
    public List<RoutineTypeIngredientResponse> getRoutineTypeIngredients(RoutineTypeCode routineTypeCode) {
        return getIngredientCandidates(routineTypeCode).stream()
                .map(candidate -> new RoutineTypeIngredientResponse(candidate.ingredientId(), candidate.name()))
                .toList();
    }

    public Optional<CurrentCourseResponse> getCurrentCourse() {
        return courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS)
                .map(course -> CurrentCourseResponse.of(course, calculateStreakDays(course.getId(), null)));
    }

    /** 다른 도메인(routine 등)이 courseId만으로 연속 지속일을 참조해야 할 때 쓰는 조회용 메서드. */
    public int getStreakDays(Long courseId) {
        return calculateStreakDays(courseId, null);
    }

    /**
     * 루틴을 지금 막 완료하는 시점(completedAt이 아직 저장되기 전)에 쓰는 조회용 메서드.
     * 완료 버튼을 누른 시각이 아니라 그 루틴이 원래 며칠차 루틴인지(routineDate)로 카운트해야
     * 자정 넘겨 늦게 완료해도 스트릭이 엉뚱한 날짜에 꽂혀 끊기지 않는다.
     */
    public int getStreakDaysAssumingCompleted(Long courseId, LocalDate routineDate) {
        return calculateStreakDays(courseId, routineDate);
    }

    private int calculateStreakDays(Long courseId, LocalDate assumeCompletedDate) {
        Set<LocalDate> completedDates = routineRepository.findByCourseIdOrderByRoutineDateDesc(courseId).stream()
                .filter(routine -> routine.getCompletedAt() != null)
                .map(Routine::getRoutineDate)
                .collect(Collectors.toSet());
        if (assumeCompletedDate != null) {
            completedDates.add(assumeCompletedDate);
        }

        LocalDate cursor = LocalDate.now();
        if (!completedDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }

        int streakDays = 0;
        while (completedDates.contains(cursor)) {
            streakDays++;
            cursor = cursor.minusDays(1);
        }
        return streakDays;
    }

    private RoutineType resolveRoutineTypeForStart(CourseType courseType, RoutineTypeCode routineTypeCode) {
        if (courseType == CourseType.FOCUS) {
            if (routineTypeCode != null) {
                throw new CustomException(CourseErrorCode.ROUTINE_TYPE_NOT_ALLOWED_FOR_FOCUS);
            }
            return null;
        }

        if (routineTypeCode == null) {
            throw new CustomException(CourseErrorCode.ROUTINE_TYPE_REQUIRED_FOR_DAILY);
        }
        return routineTypeRepository.findById(routineTypeCode)
                .orElseThrow(() -> new CustomException(CourseErrorCode.ROUTINE_TYPE_NOT_FOUND));
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
    }

    /**
     * 최근 7일(오늘 포함) 증상 중 가장 많이 선택된 키워드 상위 2개(동률이면 이름순 정렬 후 앞 2개)와,
     * 그 증상들의 매핑 세트를 교집합해 추천하는 데일리 루틴 타입을 함께 반환한다.
     */
    public CourseSymptomSummaryResponse getSymptomSummary(Long courseId) {
        getCourseOrThrow(courseId);
        List<Symptom> symptoms = routineSymptomRepository.findSymptomsByCourseIdAndDateFrom(
                courseId, LocalDate.now().minusDays(SYMPTOM_LOOKBACK_DAYS));

        List<CourseSymptomSummaryResponse.SymptomFrequency> topSymptoms = symptoms.stream()
                .filter(symptom -> symptom != Symptom.NONE)
                .collect(Collectors.groupingBy(symptom -> symptom, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Symptom, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(2)
                .map(entry -> new CourseSymptomSummaryResponse.SymptomFrequency(entry.getKey(), entry.getValue()))
                .toList();

        RoutineTypeCode recommendedCode = SymptomRoutineTypeMapper.recommend(
                topSymptoms.stream().map(CourseSymptomSummaryResponse.SymptomFrequency::symptom).toList());
        String recommendedName = recommendedCode != null
                ? routineTypeRepository.findById(recommendedCode).map(RoutineType::getName).orElse(null)
                : null;

        return new CourseSymptomSummaryResponse(topSymptoms, recommendedCode, recommendedName);
    }

    /**
     * (성분, 카테고리) 조합마다 상점 제품을 랜덤으로 1개씩 뽑는다(개수 상한 없음, 같은 조합 중복 제거).
     * 해당 조합에 제품이 하나도 없으면 결과에서 그냥 빠진다.
     */
    public List<RecommendedProductResponse> pickRecommendedProducts(List<ProductPickCandidate> candidates) {
        List<RecommendedProductResponse> picked = new ArrayList<>();
        Set<ProductPickCandidate> seenCandidates = new LinkedHashSet<>();
        for (ProductPickCandidate candidate : candidates) {
            if (!seenCandidates.add(candidate)) {
                continue;
            }
            List<ProductResponse> products = productService.getProductsByIngredient(candidate.ingredientId(), candidate.category());
            if (products.isEmpty()) {
                continue;
            }
            String ingredientName = ingredientRepository.findById(candidate.ingredientId())
                    .map(Ingredient::getName)
                    .orElse("성분");
            ProductResponse randomProduct = products.get(ThreadLocalRandom.current().nextInt(products.size()));
            picked.add(new RecommendedProductResponse(ingredientName, randomProduct));
        }
        return picked;
    }
}
