package com.likelion.duckswell.domain.diagnosis.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DiagnosisErrorCode implements ErrorCode {

    DIAGNOSIS_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D001", "AI 분석 처리 중 오류가 발생했습니다."),
    PHOTO_REQUIRED_FOR_FOCUS(HttpStatus.BAD_REQUEST, "D002", "집중 코스에서는 사진이 필수입니다."),
    PHOTO_NOT_FOUND(HttpStatus.BAD_REQUEST, "D003", "사진 파일을 처리할 수 없습니다."),
    PHOTO_NO_FACE_DETECTED(HttpStatus.BAD_REQUEST, "D004", "사진에서 얼굴을 인식하지 못했습니다. 다시 촬영해주세요."),
    PHOTO_TOO_DARK(HttpStatus.BAD_REQUEST, "D005", "사진이 너무 어둡습니다. 밝은 곳에서 다시 촬영해주세요."),
    PHOTO_TOO_BRIGHT(HttpStatus.BAD_REQUEST, "D006", "사진이 너무 밝습니다. 다시 촬영해주세요."),
    PHOTO_UNEVEN_LIGHTING(HttpStatus.BAD_REQUEST, "D007", "조명이 고르지 않습니다. 다시 촬영해주세요."),
    PHOTO_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D008", "사진 분석 중 오류가 발생했습니다."),
    LLM_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "D009", "AI 응답을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DiagnosisErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
