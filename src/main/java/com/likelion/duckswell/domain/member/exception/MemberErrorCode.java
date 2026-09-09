package com.likelion.duckswell.domain.member.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "M002", "게스트 인증 정보가 없습니다. 게스트로 다시 시작해주세요."),
    INVALID_GUEST_TOKEN(HttpStatus.UNAUTHORIZED, "M003", "유효하지 않은 게스트 토큰입니다. 게스트로 다시 시작해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MemberErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
