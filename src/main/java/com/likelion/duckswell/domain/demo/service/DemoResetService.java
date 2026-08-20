package com.likelion.duckswell.domain.demo.service;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeRepository;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;
import com.likelion.duckswell.domain.dashboard.entity.ChecklistSourceType;
import com.likelion.duckswell.domain.dashboard.repository.ChecklistItemRepository;
import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import com.likelion.duckswell.domain.diagnosis.repository.DiagnosisRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.procedure.entity.Procedure;
import com.likelion.duckswell.domain.procedure.entity.ProcedureAreaType;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import com.likelion.duckswell.domain.procedure.repository.ProcedureRepository;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import com.likelion.duckswell.domain.routine.entity.IngredientRole;
import com.likelion.duckswell.domain.routine.entity.Routine;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import com.likelion.duckswell.domain.routine.entity.RoutineStep;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import com.likelion.duckswell.domain.routine.repository.RoutineRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 심사(시연) 기간 동안 배포 링크에 다른 참가자가 직접 들어와 앱을 조작해도, 화면의 리셋 버튼
 * 하나로 정해진 시연 시나리오 상태로 되돌릴 수 있게 하기 위한 데모 전용 리셋 기능.
 * member(id=1)와 마스터 데이터(routine_type/ingredient/product 등)는 건드리지 않고,
 * 코스~체크리스트까지 회원이 실제로 앱을 쓰며 쌓는 데이터만 지운 뒤 고정 시나리오로 재시딩한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DemoResetService {

    private static final RoutineTypeCode DEMO_ROUTINE_TYPE_CODE = RoutineTypeCode.COOLDOWN;
    private static final ProcedureType DEMO_PROCEDURE_TYPE = ProcedureType.EXTRACTION_INJECTION;
    private static final ProcedureAreaType DEMO_PROCEDURE_AREA = ProcedureAreaType.FULL_FACE;

    /** 오늘을 시술 후 7일차로 맞추기 위한 시술일 오프셋 (dayNumber = 경과일 + 1 로 계산되므로 6일 전). */
    private static final int PROCEDURE_DAYS_AGO = 6;

    private final CourseRepository courseRepository;
    private final ProcedureRepository procedureRepository;
    private final RoutineRepository routineRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final RoutineTypeRepository routineTypeRepository;
    private final IngredientRepository ingredientRepository;

    public void reset() {
        clearMemberData();
        seedDemoScenario();
    }

    private void clearMemberData() {
        List<Course> courses = courseRepository.findByMemberIdOrderByStartedAtDescIdDesc(Member.DEFAULT_ID);
        checklistItemRepository.deleteByMemberId(Member.DEFAULT_ID);

        for (Course course : courses) {
            List<Routine> routines = routineRepository.findByCourseIdOrderByRoutineDateDesc(course.getId());
            List<Long> routineIds = routines.stream().map(Routine::getId).toList();
            if (!routineIds.isEmpty()) {
                diagnosisRepository.deleteByRoutineIdIn(routineIds);
            }
            routineRepository.deleteAll(routines);
            procedureRepository.deleteAll(
                    procedureRepository.findByMemberIdAndCourseIdOrderByProcedureDateDesc(Member.DEFAULT_ID, course.getId()));
        }
        courseRepository.deleteAll(courses);
    }

    private void seedDemoScenario() {
        LocalDate today = LocalDate.now();
        LocalDate procedureDate = today.minusDays(PROCEDURE_DAYS_AGO);

        RoutineType routineType = routineTypeRepository.findById(DEMO_ROUTINE_TYPE_CODE).orElseThrow();
        Course course = courseRepository.save(new Course(Member.DEFAULT_ID, null, CourseType.FOCUS, routineType, procedureDate));

        Procedure procedure = new Procedure(Member.DEFAULT_ID, course.getId(), DEMO_PROCEDURE_TYPE, procedureDate, 1, 1);
        procedure.addArea(DEMO_PROCEDURE_AREA);
        procedureRepository.save(procedure);

        Long hyaluronicAcidId = requireIngredientId("히알루론산");
        Long centellaId = requireIngredientId("센텔라");
        Long panthenolId = requireIngredientId("판테놀");

        for (DemoDay day : buildDemoDays(hyaluronicAcidId, centellaId, panthenolId)) {
            seedDay(course.getId(), today, day);
        }

        checklistItemRepository.save(new ChecklistItem(
                Member.DEFAULT_ID, course.getId(), today, 0,
                "간접적 자외선 차단하기",
                "압출 시술 후에는 피부가 예민할 수 있으니 모자와 마스크로 직접적인 자외선 노출을 피해주세요.",
                ChecklistSourceType.PROCEDURE_CAUTION));
        checklistItemRepository.save(new ChecklistItem(
                Member.DEFAULT_ID, course.getId(), today, 1,
                "충분한 수면과 수분 섭취",
                "회복기에는 일찍 자고 물을 자주 마시는 것이 피부 회복에 도움이 돼요.",
                ChecklistSourceType.PROCEDURE_CAUTION));
    }

    private void seedDay(Long courseId, LocalDate today, DemoDay day) {
        LocalDate routineDate = today.minusDays(day.daysAgo());
        Routine routine = new Routine(courseId, routineDate, day.photoUrl(), day.symptomNote());
        day.symptoms().forEach(routine::addSymptom);
        routine.selectDifficulty(day.difficulty(), day.estimatedMinutes(), day.reasonText());
        for (StepDef step : day.steps()) {
            RoutineStep routineStep = routine.addStep(
                    step.order(), step.name(), step.category(), step.productText(), step.methodText(), step.alternateText());
            if (step.ingredientId() != null) {
                routineStep.addIngredient(step.ingredientId(), IngredientRole.PRIMARY);
            }
        }
        routine.complete(day.completionSummaryText());
        Routine savedRoutine = routineRepository.save(routine);

        diagnosisRepository.save(new Diagnosis(
                savedRoutine.getId(), day.rednessScore(), day.textureScore(), day.blemishScore(), day.diagnosisSummaryText()));
    }

    private Long requireIngredientId(String name) {
        return ingredientRepository.findByName(name)
                .map(Ingredient::getId)
                .orElseThrow(() -> new IllegalStateException("데모 리셋에 필요한 성분 마스터 데이터가 없습니다: " + name));
    }

    /** 시술 후 1~6일차(오늘은 라이브 시연용으로 비워둠) 데모 데이터 - 회복 흐름과 붉은기/열감 위주 증상을 보여준다. */
    private List<DemoDay> buildDemoDays(Long hyaluronicAcidId, Long centellaId, Long panthenolId) {
        return List.of(
                new DemoDay(6, RoutineDifficulty.INTENSIVE, 15,
                        "시술 직후라 진정 위주로 꼼꼼하게 구성했어요", "붉고 화끈거리고 약간 부었어요", "오늘 관리를 잘 마쳤어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT, Symptom.SWELLING),
                        List.of(
                                cleanser(1), ampoule(2, "센텔라 아시아티카 앰플", "진정 앰플을 얇게 펴발라주세요", "판테놀 앰플", centellaId),
                                cream(3, "판테놀 크림", "보습크림을 충분히 발라 피부를 감싸주세요", null)),
                        78, 45, 25, "붉은기와 열감이 뚜렷한 초기 회복 단계예요"),
                new DemoDay(5, RoutineDifficulty.INTENSIVE, 15,
                        "아직 초기라 진정 케어를 유지했어요", "붓기는 좀 빠졌는데 여전히 붉고 열감이 있어요", "오늘도 관리 완료했어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT, Symptom.SWELLING),
                        List.of(
                                cleanser(1), ampoule(2, "센텔라 아시아티카 앰플", "진정 앰플을 얇게 펴발라주세요", null, centellaId),
                                cream(3, "판테놀 크림", "보습크림으로 마무리해주세요", null)),
                        68, 42, 22, "붓기는 완화됐지만 붉은기와 열감은 아직 남아있어요"),
                new DemoDay(4, RoutineDifficulty.BASIC, 10,
                        "중기 단계로 넘어가서 장벽 강화 위주로 구성했어요", "붉은기랑 열감은 남았고 약간 간지러워요", "오늘 관리 완료했어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT, Symptom.ITCHINESS),
                        List.of(
                                cleanser(1), ampoule(2, "세라마이드 앰플", "장벽 강화 앰플을 발라주세요", null, centellaId),
                                cream(3, "히알루론산 크림", "보습크림으로 마무리해주세요", hyaluronicAcidId)),
                        55, 38, 18, "붉은기와 열감이 서서히 가라앉고 있어요"),
                new DemoDay(3, RoutineDifficulty.BASIC, 10,
                        "장벽 강화와 보습 위주로 이어갔어요", "많이 가라앉았는데 붉은기랑 열감은 아직 남아있고 좀 건조해요", "오늘도 관리 완료했어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT, Symptom.DRYNESS),
                        List.of(
                                cleanser(1), ampoule(2, "세라마이드 앰플", "장벽 강화 앰플을 발라주세요", null, panthenolId),
                                cream(3, "히알루론산 크림", "보습크림을 충분히 발라주세요", hyaluronicAcidId)),
                        40, 32, 15, "붉은기와 열감이 옅어지고 있어요"),
                new DemoDay(2, RoutineDifficulty.LIGHT, 8,
                        "거의 회복돼서 가볍게 관리하는 단계예요", "붉은기랑 열감이 많이 줄었어요", "가볍게 관리 완료했어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT),
                        List.of(
                                cleanser(1), cream(2, "세라마이드 크림", "보습크림을 발라 마무리해주세요", hyaluronicAcidId)),
                        0, 32, 44, "붉은기는 거의 안 보일 정도로 가라앉았어요"),
                new DemoDay(1, RoutineDifficulty.LIGHT, 8,
                        "회복이 거의 끝나서 가볍게 마무리 관리로 구성했어요", "이제 거의 다 괜찮아졌어요", "가볍게 관리 완료했어요",
                        List.of(Symptom.REDNESS, Symptom.HEAT, Symptom.FLAKING),
                        List.of(
                                cleanser(1), cream(2, "세라마이드 크림", "보습크림을 발라 마무리해주세요", hyaluronicAcidId)),
                        32, 16, 27, "어제보다는 살짝 올라왔지만 전체적으로 안정된 편이에요")
        );
    }

    private StepDef cleanser(int order) {
        return new StepDef(order, "순한 클렌징", ProductCategory.CLEANSER, "약산성 클렌저", "미온수로 부드럽게 세안해주세요", null, null);
    }

    private StepDef ampoule(int order, String productText, String methodText, String alternateText, Long ingredientId) {
        return new StepDef(order, "진정 앰플", ProductCategory.AMPOULE_SERUM, productText, methodText, alternateText, ingredientId);
    }

    private StepDef cream(int order, String productText, String methodText, Long ingredientId) {
        return new StepDef(order, "보습 마무리", ProductCategory.CREAM, productText, methodText, null, ingredientId);
    }

    private record DemoDay(
            int daysAgo,
            RoutineDifficulty difficulty,
            int estimatedMinutes,
            String reasonText,
            String symptomNote,
            String completionSummaryText,
            List<Symptom> symptoms,
            List<StepDef> steps,
            int rednessScore,
            int textureScore,
            int blemishScore,
            String diagnosisSummaryText
    ) {
        String photoUrl() {
            return "demo-photo-d" + (6 - daysAgo + 1) + ".jpg";
        }
    }

    private record StepDef(
            int order,
            String name,
            ProductCategory category,
            String productText,
            String methodText,
            String alternateText,
            Long ingredientId
    ) {
    }
}
