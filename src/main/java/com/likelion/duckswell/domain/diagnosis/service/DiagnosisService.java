package com.likelion.duckswell.domain.diagnosis.service;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.RecoverySummaryResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.diagnosis.client.cv.CvAnalysisClient;
import com.likelion.duckswell.domain.diagnosis.client.cv.CvScoreResult;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmDiagnosisClient;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmDiagnosisContext;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmDiagnosisResult;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRecoveryStageClient;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRecoveryStageContext;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRecoveryStageResult;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineCompletionClient;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineCompletionContext;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineCompletionResult;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsClient;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsContext;
import com.likelion.duckswell.domain.diagnosis.client.llm.LlmRoutineStepsResult;
import com.likelion.duckswell.domain.diagnosis.client.cv.PhotoQualityResult;
import com.likelion.duckswell.domain.diagnosis.client.storage.PhotoStorage;
import com.likelion.duckswell.domain.diagnosis.dto.DifficultyOptionResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisScoreSnapshot;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.duckswell.domain.diagnosis.dto.PhotoCheckResponse;
import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.diagnosis.repository.DiagnosisRepository;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.domain.routine.dto.RoutineCompleteResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.dto.RoutineStepsResponse;
import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private final CourseService courseService;
    private final RoutineService routineService;
    private final ProcedureService procedureService;
    private final DiagnosisRepository diagnosisRepository;
    private final PhotoStorage photoStorage;
    private final CvAnalysisClient cvAnalysisClient;
    private final LlmDiagnosisClient llmDiagnosisClient;
    private final LlmRoutineStepsClient llmRoutineStepsClient;
    private final LlmRoutineCompletionClient llmRoutineCompletionClient;
    private final LlmRecoveryStageClient llmRecoveryStageClient;

    /**
     * 사진은 여기서 딱 한 번 업로드/저장되고 품질을 확인한다. 통과한 photoId를 제출 시점에 재사용한다.
     * DB 쓰기가 없는데도 클래스 기본값(readOnly 트랜잭션)에 걸리지 않도록 명시적으로 트랜잭션 밖에서 실행한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PhotoCheckResponse checkPhoto(MultipartFile photo) {
        String photoId = photoStorage.save(photo);
        String resolvedPath = null;
        boolean accepted = false;
        try {
            resolvedPath = photoStorage.resolvePath(photoId);
            PhotoQualityResult quality = cvAnalysisClient.checkPhotoQuality(resolvedPath);
            if (!quality.ok()) {
                throw new CustomException(mapQualityReason(quality.reason()));
            }
            accepted = true;
            return new PhotoCheckResponse(photoId);
        } finally {
            if (resolvedPath != null) {
                photoStorage.cleanupResolvedPath(resolvedPath);
            }
            if (!accepted) {
                try {
                    photoStorage.delete(photoId);
                } catch (RuntimeException e) {
                    log.warn("품질 체크 실패한 사진 삭제 실패: photoId={}", photoId, e);
                }
            }
        }
    }

    /**
     * CV 서브프로세스(최대 30초)와 LLM 호출이 끝날 때까지 DB 커넥션을 붙잡고 있지 않도록,
     * 외부 호출 구간은 트랜잭션 밖에서 실행한다. 이후 루틴 생성/진단 저장은 각 저장소 호출이
     * 자체적으로 트랜잭션을 여닫는다(RoutineService, JpaRepository 모두 자체 @Transactional 보유).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DiagnosisResponse submitDiagnosis(DiagnosisSubmitRequest request) {
        CourseResponse course = courseService.getCourse(request.courseId());
        boolean hasPhoto = request.photoId() != null;

        if (course.courseType() == CourseType.FOCUS && !hasPhoto) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_REQUIRED_FOR_FOCUS);
        }

        // photoId는 checkPhoto()에서 이미 품질 검증을 통과한 사진이라 여기서 다시 확인하지 않는다.
        String photoPath = null;
        CvScoreResult scores = null;
        LlmDiagnosisResult llmResult;
        try {
            if (hasPhoto) {
                photoPath = photoStorage.resolvePath(request.photoId());
                scores = cvAnalysisClient.analyze(photoPath);
            }

            // 데일리 코스는 사진 분석이 선택 사항이라 전날과 비교하지 않고 항상 현재 상태만
            // 요약한다. 어제 컨텍스트는 FOCUS 코스일 때만 채운다.
            LlmDiagnosisContext context = new LlmDiagnosisContext(
                    photoPath,
                    request.symptoms(),
                    request.symptomNote(),
                    scores,
                    course.courseType() == CourseType.FOCUS ? buildYesterdayContext(request.courseId()) : null,
                    procedureService.getMyProcedures()
            );
            llmResult = llmDiagnosisClient.analyze(context);
        } finally {
            // S3처럼 resolvePath()가 로컬에 임시 파일을 만든 구현체만 실제로 지운다(로컬 저장소는 no-op).
            if (photoPath != null) {
                photoStorage.cleanupResolvedPath(photoPath);
            }
        }

        RoutineSnapshot routine = routineService.createTodayRoutine(
                request.courseId(), photoPath, request.symptomNote(), request.symptoms());

        Diagnosis diagnosis = new Diagnosis(
                routine.id(),
                toIntegerOrNull(scores != null ? scores.rednessPct() : null),
                toIntegerOrNull(scores != null ? scores.texturePct() : null),
                toIntegerOrNull(scores != null ? scores.blemishPct() : null),
                llmResult.summaryText()
        );
        diagnosisRepository.save(diagnosis);

        List<DifficultyOptionResponse> options = llmResult.difficultyOptions().stream()
                .map(DifficultyOptionResponse::from)
                .toList();
        return DiagnosisResponse.of(diagnosis, options);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RoutineStepsResponse selectDifficulty(Long routineId, RoutineDifficulty difficulty) {
        RoutineSnapshot routine = routineService.getRoutineSnapshot(routineId);
        CourseResponse course = courseService.getCourse(routine.courseId());
        List<LlmRoutineStepsContext.IngredientCandidate> candidates =
                routineService.resolveIngredientCandidates(course.routineTypeCode());
        Diagnosis diagnosis = diagnosisRepository.findByRoutineId(routineId).orElse(null);

        LlmRoutineStepsContext context = new LlmRoutineStepsContext(
                difficulty,
                routine.symptoms(),
                routine.symptomNote(),
                diagnosis != null ? diagnosis.getRednessScore() : null,
                diagnosis != null ? diagnosis.getTextureScore() : null,
                diagnosis != null ? diagnosis.getBlemishScore() : null,
                diagnosis != null ? diagnosis.getSummaryText() : null,
                procedureService.getMyProcedures(),
                candidates
        );
        LlmRoutineStepsResult result = llmRoutineStepsClient.generate(context);

        return routineService.applyDifficultySelection(routineId, difficulty, result);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RoutineCompleteResponse completeRoutine(Long routineId) {
        RoutineSnapshot routine = routineService.getRoutineSnapshot(routineId);
        CourseResponse course = courseService.getCourse(routine.courseId());
        List<LlmRoutineCompletionContext.CompletedStep> completedSteps = routineService.getCompletedStepsInfo(routineId);

        LlmRoutineCompletionContext context = new LlmRoutineCompletionContext(
                course.courseType(),
                course.routineTypeName(),
                course.courseType() == CourseType.DAILY
                        ? courseService.getStreakDaysAssumingCompleted(routine.courseId(), routine.routineDate())
                        : null,
                completedSteps
        );
        LlmRoutineCompletionResult result = llmRoutineCompletionClient.summarize(context);
        return routineService.applyCompletion(routineId, result.completionSummaryText());
    }

    /**
     * 어제 루틴이 완료된 경우 "어제 요약 + 오늘 회복 단계"를, 아니면 "오늘 회복 단계"만 서술한다.
     * 두 경우 모두 캐시된 값이 있으면 LLM을 다시 호출하지 않고 그대로 반환한다
     * (어제 기록이 없는 경우는 오늘 루틴 row에 캐싱한다 - 오늘 루틴이 아직 없으면 캐싱할 대상이 없어 매번 새로 생성한다).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RecoverySummaryResponse getRecoverySummary(Long courseId) {
        CourseResponse course = courseService.getCourse(courseId);
        LocalDate today = LocalDate.now();
        int dayNumber = (int) ChronoUnit.DAYS.between(course.startedAt(), today) + 1;

        Optional<RoutineSnapshot> yesterday = routineService.findRoutineSnapshot(courseId, today.minusDays(1));
        boolean hasYesterday = yesterday.isPresent() && yesterday.get().completedAt() != null;

        if (hasYesterday) {
            RoutineSnapshot routine = yesterday.get();
            if (routine.recoveryStageSummaryText() != null) {
                return new RecoverySummaryResponse(routine.recoveryStageSummaryText());
            }
            LlmRecoveryStageResult result = llmRecoveryStageClient.summarize(new LlmRecoveryStageContext(
                    dayNumber, true, routine.symptoms(), routine.symptomNote(), routine.completionSummaryText()));
            routineService.cacheRecoveryStageSummary(routine.id(), result.recoveryStageSummaryText());
            return new RecoverySummaryResponse(result.recoveryStageSummaryText());
        }

        Optional<RoutineSnapshot> todayRoutine = routineService.findRoutineSnapshot(courseId, today);
        if (todayRoutine.isPresent() && todayRoutine.get().recoveryStageSummaryText() != null) {
            return new RecoverySummaryResponse(todayRoutine.get().recoveryStageSummaryText());
        }

        LlmRecoveryStageResult result = llmRecoveryStageClient.summarize(
                new LlmRecoveryStageContext(dayNumber, false, List.of(), null, null));
        todayRoutine.ifPresent(routine -> routineService.cacheRecoveryStageSummary(routine.id(), result.recoveryStageSummaryText()));
        return new RecoverySummaryResponse(result.recoveryStageSummaryText());
    }

    /**
     * courseId의 특정 날짜 진단 점수를 조회한다 - 그 날짜에 루틴 기록이 없거나, 기록은 있어도
     * 진단(사진 분석)을 안 했거나, 점수 중 일부가 비어 있으면 빈 값을 반환한다(집중 코스 홈 배너의
     * 오늘/어제 점수 비교에 사용 - dashboard 도메인에서 호출).
     */
    public Optional<DiagnosisScoreSnapshot> getScores(Long courseId, LocalDate date) {
        Optional<RoutineSnapshot> routine = routineService.findRoutineSnapshot(courseId, date);
        if (routine.isEmpty()) {
            return Optional.empty();
        }

        Optional<Diagnosis> diagnosis = diagnosisRepository.findByRoutineId(routine.get().id());
        if (diagnosis.isEmpty()) {
            return Optional.empty();
        }

        Diagnosis found = diagnosis.get();
        if (found.getRednessScore() == null || found.getTextureScore() == null || found.getBlemishScore() == null) {
            return Optional.empty();
        }

        return Optional.of(new DiagnosisScoreSnapshot(found.getRednessScore(), found.getTextureScore(), found.getBlemishScore()));
    }

    private DiagnosisErrorCode mapQualityReason(String reason) {
        if (reason == null) {
            return DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED;
        }
        return switch (reason) {
            case "no_face_detected" -> DiagnosisErrorCode.PHOTO_NO_FACE_DETECTED;
            case "too_dark" -> DiagnosisErrorCode.PHOTO_TOO_DARK;
            case "too_bright" -> DiagnosisErrorCode.PHOTO_TOO_BRIGHT;
            case "uneven_lighting" -> DiagnosisErrorCode.PHOTO_UNEVEN_LIGHTING;
            case "image_not_found" -> DiagnosisErrorCode.PHOTO_NOT_FOUND;
            default -> DiagnosisErrorCode.PHOTO_ANALYSIS_FAILED;
        };
    }

    private LlmDiagnosisContext.YesterdayContext buildYesterdayContext(Long courseId) {
        Optional<RoutineSnapshot> yesterdayRoutine = routineService.findRoutineSnapshot(courseId, LocalDate.now().minusDays(1));
        if (yesterdayRoutine.isEmpty()) {
            return null;
        }
        RoutineSnapshot routine = yesterdayRoutine.get();
        Optional<Diagnosis> yesterdayDiagnosis = diagnosisRepository.findByRoutineId(routine.id());

        CvScoreResult yesterdayScores = null;
        String yesterdaySummary = null;
        if (yesterdayDiagnosis.isPresent()) {
            Diagnosis diagnosis = yesterdayDiagnosis.get();
            yesterdaySummary = diagnosis.getSummaryText();
            if (diagnosis.getRednessScore() != null && diagnosis.getTextureScore() != null && diagnosis.getBlemishScore() != null) {
                yesterdayScores = new CvScoreResult(diagnosis.getRednessScore(), diagnosis.getBlemishScore(), diagnosis.getTextureScore());
            }
        }

        return new LlmDiagnosisContext.YesterdayContext(routine.symptoms(), routine.symptomNote(), yesterdayScores, yesterdaySummary);
    }

    private Integer toIntegerOrNull(Double value) {
        return value != null ? (int) Math.round(value) : null;
    }
}
