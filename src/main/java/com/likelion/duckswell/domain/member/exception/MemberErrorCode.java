package com.likelion.duckswell.domain.member.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MemberErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
