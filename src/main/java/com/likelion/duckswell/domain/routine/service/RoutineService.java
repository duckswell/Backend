package com.likelion.duckswell.domain.routine.service;

import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.entity.Routine;
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
}
