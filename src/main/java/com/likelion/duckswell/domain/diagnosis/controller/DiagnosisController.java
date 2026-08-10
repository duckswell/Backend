package com.likelion.duckswell.domain.diagnosis.controller;

import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisResponse;
import com.likelion.duckswell.domain.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.duckswell.domain.diagnosis.dto.PhotoCheckResponse;
import com.likelion.duckswell.domain.diagnosis.service.DiagnosisService;
import com.likelion.duckswell.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController implements DiagnosisApi {

    private final DiagnosisService diagnosisService;

    @Override
    @PostMapping(value = "/photo-check", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PhotoCheckResponse>> checkPhoto(@RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(ApiResponse.success(diagnosisService.checkPhoto(photo)));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<DiagnosisResponse>> submitDiagnosis(@Valid @RequestBody DiagnosisSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(diagnosisService.submitDiagnosis(request)));
    }
}
