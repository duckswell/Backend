package com.likelion.duckswell.domain.diagnosis.service;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.entity.CourseType;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.domain.diagnosis.client.CvAnalysisClient;
import com.likelion.duckswell.domain.diagnosis.client.CvScoreResult;
import com.likelion.duckswell.domain.diagnosis.client.LlmDiagnosisClient;
import com.likelion.duckswell.domain.diagnosis.client.LlmDiagnosisContext;
import com.likelion.duckswell.domain.diagnosis.client.LlmDiagnosisResult;
import com.likelion.duckswell.domain.diagnosis.client.PhotoQualityResult;
import com.likelion.duckswell.domain.diagnosis.client.PhotoStorage;
import com.likelion.duckswell.domain.diagnosis.dto.DifficultyOptionResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.duckswell.domain.diagnosis.dto.PhotoCheckResponse;
import com.likelion.duckswell.domain.diagnosis.entity.Diagnosis;
import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.domain.diagnosis.repository.DiagnosisRepository;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.domain.routine.dto.RoutineSnapshot;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    /** 사진은 여기서 딱 한 번 업로드/저장되고 품질을 확인한다. 통과한 photoId를 제출 시점에 재사용한다. */
    @Transactional
    public PhotoCheckResponse checkPhoto(MultipartFile photo) {
        String photoId = photoStorage.save(photo);
        PhotoQualityResult quality = cvAnalysisClient.checkPhotoQuality(photoStorage.resolvePath(photoId));
        if (!quality.ok()) {
            photoStorage.delete(photoId);
            throw new CustomException(mapQualityReason(quality.reason()));
        }
        return new PhotoCheckResponse(photoId);
    }

    @Transactional
    public DiagnosisResponse submitDiagnosis(DiagnosisSubmitRequest request) {
        CourseResponse course = courseService.getCourse(request.courseId());
        boolean hasPhoto = request.photoId() != null;

        if (course.courseType() == CourseType.FOCUS && !hasPhoto) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_REQUIRED_FOR_FOCUS);
        }

        // photoId는 checkPhoto()에서 이미 품질 검증을 통과한 사진이라 여기서 다시 확인하지 않는다.
        String photoPath = null;
        CvScoreResult scores = null;
        if (hasPhoto) {
            photoPath = photoStorage.resolvePath(request.photoId());
            scores = cvAnalysisClient.analyze(photoPath);
        }

        RoutineSnapshot routine = routineService.createTodayRoutine(
                request.courseId(), photoPath, request.symptomNote(), request.symptoms());

        LlmDiagnosisContext context = new LlmDiagnosisContext(
                photoPath,
                request.symptoms(),
                request.symptomNote(),
                scores,
                buildYesterdayContext(request.courseId()),
                procedureService.getMyProcedures()
        );
        LlmDiagnosisResult llmResult = llmDiagnosisClient.analyze(context);

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
