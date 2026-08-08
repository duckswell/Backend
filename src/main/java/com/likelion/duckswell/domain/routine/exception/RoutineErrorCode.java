package com.likelion.duckswell.domain.routine.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RoutineErrorCode implements ErrorCode {

    SYMPTOM_CHECK_REQUIRED(HttpStatus.BAD_REQUEST, "R001", "증상 체크는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    RoutineErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
