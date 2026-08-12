package com.likelion.duckswell.domain.dashboard.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DashboardErrorCode implements ErrorCode {

    LLM_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "DA001", "AI 체크리스트 응답을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DashboardErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
