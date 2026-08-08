package com.likelion.duckswell.domain.procedure.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProcedureErrorCode implements ErrorCode {

    PROCEDURE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "등록된 시술 정보가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProcedureErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
