package com.likelion.duckswell.domain.diagnosis.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DiagnosisErrorCode implements ErrorCode {

    DIAGNOSIS_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D001", "AI 분석 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DiagnosisErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
