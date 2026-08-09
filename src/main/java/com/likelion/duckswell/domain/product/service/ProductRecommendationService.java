package com.likelion.duckswell.domain.product.service;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.product.dto.IngredientResponse;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import com.likelion.duckswell.domain.routine.repository.RoutineStepIngredientRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRecommendationService {

    /** 진행중인 코스가 없을 때(공백기), 직전 코스에서 거슬러 올라갈 최근 루틴 개수. */
    private static final int FALLBACK_ROUTINE_LIMIT = 3;

    /** 추천 성분으로 반환할 최대 개수 — 가장 많이 등장한 순으로 상위 N개. */
    private static final int RECOMMENDATION_LIMIT = 3;

    private final CourseRepository courseRepository;
    private final RoutineRepository routineRepository;
    private final RoutineStepIngredientRepository routineStepIngredientRepository;
    private final IngredientRepository ingredientRepository;

    public List<IngredientResponse> getRecommendedIngredients() {
        List<Long> orderedIngredientIds = resolveRecommendedIngredientIds();
        List<Long> topIngredientIds = orderedIngredientIds.stream().limit(RECOMMENDATION_LIMIT).toList();
        if (topIngredientIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Ingredient> ingredientsById = ingredientRepository.findAllById(topIngredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        return topIngredientIds.stream()
                .map(ingredientsById::get)
                .map(IngredientResponse::from)
                .toList();
    }

    private List<Long> resolveRecommendedIngredientIds() {
        Optional<Course> activeCourse = courseRepository.findByMemberIdAndStatus(Member.DEFAULT_ID, CourseStatus.IN_PROGRESS);
        if (activeCourse.isPresent()) {
            List<Routine> routines = routineRepository.findByCourseIdOrderByRoutineDateDesc(activeCourse.get().getId());
            log.info("추천 성분 근거 - basis=ACTIVE_COURSE, courseId={}, routineCount={}", activeCourse.get().getId(), routines.size());
            return collectIngredientIds(routines);
        }

        Optional<Course> fallbackCourse = courseRepository.findFirstByMemberIdAndStatusOrderByEndedAtDesc(Member.DEFAULT_ID, CourseStatus.COMPLETED);
        if (fallbackCourse.isEmpty()) {
            log.info("추천 성분 근거 - basis=NO_HISTORY, memberId={}", Member.DEFAULT_ID);
            return List.of();
        }
        Long fallbackCourseId = fallbackCourse.get().getId();
        List<Routine> routines = routineRepository.findByCourseIdOrderByRoutineDateDesc(fallbackCourseId);
        List<Routine> recentRoutines = routines.subList(0, Math.min(FALLBACK_ROUTINE_LIMIT, routines.size()));
        log.info("추천 성분 근거 - basis=FALLBACK_RECENT_ROUTINE, courseId={}, usedRoutineIds={}",
                fallbackCourseId, recentRoutines.stream().map(Routine::getId).toList());
        return collectIngredientIds(recentRoutines);
    }

    private List<Long> collectIngredientIds(List<Routine> routines) {
        if (routines.isEmpty()) {
            return List.of();
        }
        List<Long> routineIds = routines.stream().map(Routine::getId).toList();
        return routineStepIngredientRepository.findIngredientIdsOrderByFrequencyDesc(routineIds);
    }
}
