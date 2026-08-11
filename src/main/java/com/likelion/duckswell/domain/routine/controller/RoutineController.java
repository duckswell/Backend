package com.likelion.duckswell.domain.routine.controller;

import com.likelion.duckswell.domain.course.dto.RecommendedProductResponse;
import com.likelion.duckswell.domain.diagnosis.service.DiagnosisService;
import com.likelion.duckswell.domain.routine.dto.RoutineCompleteResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineStepSummaryResponse;
import com.likelion.duckswell.domain.routine.dto.RoutineStepsResponse;
import com.likelion.duckswell.domain.routine.dto.SelectDifficultyRequest;
import com.likelion.duckswell.domain.routine.dto.TodayRoutineResponse;
import com.likelion.duckswell.domain.routine.service.RoutineService;
import com.likelion.duckswell.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class RoutineController implements RoutineApi {

    private final RoutineService routineService;
    private final DiagnosisService diagnosisService;

    @Override
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayRoutineResponse>> getTodayRoutine() {
        return ResponseEntity.ok(ApiResponse.success(
                routineService.getTodayRoutineId().map(TodayRoutineResponse::new).orElse(null)));
    }

    @Override
    @PostMapping("/{routineId}/difficulty")
    public ResponseEntity<ApiResponse<RoutineStepsResponse>> selectDifficulty(
            @PathVariable Long routineId, @Valid @RequestBody SelectDifficultyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(diagnosisService.selectDifficulty(routineId, request.difficulty())));
    }

    @Override
    @PostMapping("/{routineId}/complete")
    public ResponseEntity<ApiResponse<RoutineCompleteResponse>> completeRoutine(@PathVariable Long routineId) {
        return ResponseEntity.ok(ApiResponse.success(diagnosisService.completeRoutine(routineId)));
    }

    @Override
    @GetMapping("/{routineId}/steps")
    public ResponseEntity<ApiResponse<List<RoutineStepSummaryResponse>>> getStepSummaries(@PathVariable Long routineId) {
        return ResponseEntity.ok(ApiResponse.success(routineService.getStepSummaries(routineId)));
    }

    @Override
    @GetMapping("/{routineId}/recommended-products")
    public ResponseEntity<ApiResponse<List<RecommendedProductResponse>>> getRecommendedProducts(@PathVariable Long routineId) {
        return ResponseEntity.ok(ApiResponse.success(routineService.getRecommendedProducts(routineId)));
    }
}
