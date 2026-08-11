package com.likelion.duckswell.domain.routine.service;

import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.entity.RoutineTypeIngredient;
import com.likelion.duckswell.domain.course.repository.RoutineTypeIngredientRepository;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineCompletionContext;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsContext.IngredientCandidate;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsResult;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientTag;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.product.repository.IngredientTagRepository;
import com.likelion.duckswell.domain.routine.dto.RoutineCompleteResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.dto.RoutineStepsResponse;
import com.likelion.duckswell.domain.routine.entity.IngredientRole;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import com.likelion.duckswell.domain.routine.entity.RoutineStep;
import com.likelion.duckswell.domain.routine.entity.RoutineStepIngredient;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.domain.routine.exception.RoutineErrorCode;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final RoutineTypeIngredientRepository routineTypeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientTagRepository ingredientTagRepository;
    private final CourseService courseService;

    /** 하루에 여러 번 기록될 수 있다 - 항상 새 row로 남긴다(수정/덮어쓰기 아님). */
    @Transactional
    public RoutineSnapshot createTodayRoutine(Long courseId, String photoUrl, String symptomNote, List<Symptom> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            throw new CustomException(RoutineErrorCode.SYMPTOM_CHECK_REQUIRED);
        }

        Routine routine = new Routine(courseId, LocalDate.now(), photoUrl, symptomNote);
        symptoms.forEach(routine::addSymptom);
        return RoutineSnapshot.from(routineRepository.save(routine));
    }

    /** 해당 날짜에 기록이 여러 건이면 가장 최근 것을 반환한다. */
    public Optional<RoutineSnapshot> findRoutineSnapshot(Long courseId, LocalDate routineDate) {
        return routineRepository.findFirstByCourseIdAndRoutineDateOrderByCreatedAtDesc(courseId, routineDate)
                .map(RoutineSnapshot::from);
    }

    public RoutineSnapshot getRoutineSnapshot(Long routineId) {
        return RoutineSnapshot.from(getRoutineOrThrow(routineId));
    }

    /** 현재 진행중인 코스의 오늘 루틴 id (재접속 등으로 routineId를 다시 얻기 위한 조회). */
    public Optional<Long> getTodayRoutineId() {
        return courseService.getCurrentCourse()
                .flatMap(course -> routineRepository.findFirstByCourseIdAndRoutineDateOrderByCreatedAtDesc(
                        course.courseId(), LocalDate.now()))
                .map(Routine::getId);
    }

    /** routineTypeCode가 null(FOCUS 코스)이면 확정 8개 성분 전체를 후보군으로 fallback한다. */
    public List<IngredientCandidate> resolveIngredientCandidates(RoutineTypeCode routineTypeCode) {
        List<Ingredient> ingredients = routineTypeCode != null
                ? ingredientRepository.findAllById(
                        routineTypeIngredientRepository.findByRoutineType_Code(routineTypeCode).stream()
                                .map(RoutineTypeIngredient::getIngredientId)
                                .toList())
                : ingredientRepository.findAll();

        return ingredients.stream()
                .map(ingredient -> new IngredientCandidate(
                        ingredient.getId(),
                        ingredient.getName(),
                        ingredientTagRepository.findByIngredientId(ingredient.getId()).stream()
                                .map(IngredientTag::getTag)
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public RoutineStepsResponse applyDifficultySelection(Long routineId, RoutineDifficulty difficulty, LlmRoutineStepsResult result) {
        Routine routine = getRoutineOrThrow(routineId);
        routine.selectDifficulty(difficulty, result.estimatedMinutes(), result.reasonText());
        routine.resetSteps();

        int order = 1;
        for (LlmRoutineStepsResult.StepResult stepResult : result.steps()) {
            RoutineStep step = routine.addStep(
                    order++, stepResult.stepName(), stepResult.category(),
                    stepResult.productText(), stepResult.methodText(), stepResult.alternateText());
            for (Long ingredientId : stepResult.ingredientIds()) {
                step.addIngredient(ingredientId, IngredientRole.PRIMARY);
            }
        }

        // RoutineStep.id는 IDENTITY 전략이라 flush가 일어나야 채워진다 - 커밋(메서드 리턴 이후)까지
        // 기다리면 아래 응답 DTO를 만드는 시점엔 아직 null이라 명시적으로 flush한다.
        return RoutineStepsResponse.from(routineRepository.saveAndFlush(routine));
    }

    public List<LlmRoutineCompletionContext.CompletedStep> getCompletedStepsInfo(Long routineId) {
        Routine routine = getRoutineOrThrow(routineId);
        if (routine.getSteps().isEmpty()) {
            throw new CustomException(RoutineErrorCode.DIFFICULTY_NOT_SELECTED);
        }

        return routine.getSteps().stream()
                .map(step -> new LlmRoutineCompletionContext.CompletedStep(
                        step.getStepName(),
                        step.getIngredients().stream()
                                .map(ingredient -> resolveIngredientName(ingredient.getIngredientId()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public RoutineCompleteResponse applyCompletion(Long routineId, String completionSummaryText) {
        Routine routine = getRoutineOrThrow(routineId);
        routine.complete(completionSummaryText);

        List<String> recommendedIngredients = routine.getSteps().stream()
                .flatMap(step -> step.getIngredients().stream())
                .map(RoutineStepIngredient::getIngredientId)
                .distinct()
                .map(this::resolveIngredientName)
                .toList();

        return RoutineCompleteResponse.of(routine, recommendedIngredients);
    }

    private String resolveIngredientName(Long ingredientId) {
        return ingredientRepository.findById(ingredientId).map(Ingredient::getName).orElse("성분");
    }

    private Routine getRoutineOrThrow(Long routineId) {
        return routineRepository.findById(routineId)
                .orElseThrow(() -> new CustomException(RoutineErrorCode.ROUTINE_NOT_FOUND));
    }
}
