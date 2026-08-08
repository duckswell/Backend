package com.likelion.duckswell.domain.course.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CourseErrorCode implements ErrorCode {

    COURSE_SWITCH_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CO001", "집중 코스 시작 후 3일 이내에는 데일리 코스로 전환할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CourseErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
