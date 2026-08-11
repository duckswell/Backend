package com.likelion.duckswell.domain.course.util;

import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.routine.entity.Symptom;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * day-after-detailed-flow-spec.md "7일 고민 키워드 → 데일리 루틴타입 자동추천 매핑" 원안(팀 최종 확정,
 * 2026-08-09) 그대로: 증상 하나가 여러 루틴타입에 걸칠 수 있어(예: 각질 = 수분충전+클리어업, 나이아신아마이드
 * 성분이 겹쳐서) 증상→타입 1:1 매핑이 아니라 "증상별 후보 집합의 교집합" 방식을 쓴다.
 */
public final class SymptomRoutineTypeMapper {

    private SymptomRoutineTypeMapper() {
    }

    private static final Map<Symptom, Set<RoutineTypeCode>> SYMPTOM_ROUTINE_TYPES = Map.of(
            Symptom.REDNESS, Set.of(RoutineTypeCode.COOLDOWN),
            Symptom.HEAT, Set.of(RoutineTypeCode.COOLDOWN),
            Symptom.OILINESS, Set.of(RoutineTypeCode.SEBUM_CONTROL),
            Symptom.DRYNESS, Set.of(RoutineTypeCode.HYDRATION),
            Symptom.FLAKING, Set.of(RoutineTypeCode.HYDRATION, RoutineTypeCode.CLEAR_UP)
            // STINGING, ITCHINESS, SWELLING: 매핑 없음(빈 집합) - 집중 코스 루틴 구성 참고용일 뿐
            // 이 데일리 추천 로직에는 쓰지 않는다(팀 확정 사항).
    );

    /** 교집합이 없으면(매핑 없는 증상이 섞이거나 후보가 서로 안 겹치면) 기본값 CLEAR_UP. */
    private static final RoutineTypeCode DEFAULT_ROUTINE_TYPE = RoutineTypeCode.CLEAR_UP;

    /**
     * top2(또는 그 이하) 증상 각각의 후보 집합을 교집합해서 추천 타입 하나를 정한다.
     * symptoms가 비어 있으면 추천할 근거 자체가 없으므로 null을 반환한다.
     */
    public static RoutineTypeCode recommend(List<Symptom> topSymptoms) {
        if (topSymptoms.isEmpty()) {
            return null;
        }

        Set<RoutineTypeCode> intersection = null;
        for (Symptom symptom : topSymptoms) {
            Set<RoutineTypeCode> mapped = SYMPTOM_ROUTINE_TYPES.getOrDefault(symptom, Set.of());
            if (intersection == null) {
                intersection = new HashSet<>(mapped);
            } else {
                intersection.retainAll(mapped);
            }
        }

        return intersection.isEmpty()
                ? DEFAULT_ROUTINE_TYPE
                : intersection.stream().min(Comparator.comparing(Enum::name)).orElse(DEFAULT_ROUTINE_TYPE);
    }
}
