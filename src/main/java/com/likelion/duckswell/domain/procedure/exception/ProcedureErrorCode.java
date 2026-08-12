package com.likelion.duckswell.domain.procedure.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProcedureErrorCode implements ErrorCode {

    PROCEDURE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "등록된 시술 정보가 없습니다."),
    FOCUS_COURSE_REQUIRED(HttpStatus.BAD_REQUEST, "P002", "시술 정보는 집중 코스 진행 중에만 등록할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProcedureErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
