package com.likelion.duckswell.domain.course.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CourseErrorCode implements ErrorCode {

    COURSE_SWITCH_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CO001", "집중 코스 시작 후 3일 이내에는 데일리 코스로 전환할 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CO002", "코스를 찾을 수 없습니다."),
    ACTIVE_COURSE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CO003", "이미 진행 중인 코스가 있습니다."),
    COURSE_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "CO004", "이미 종료된 코스입니다."),
    ROUTINE_TYPE_REQUIRED_FOR_DAILY(HttpStatus.BAD_REQUEST, "CO005", "데일리 코스는 루틴 타입을 반드시 선택해야 합니다."),
    ROUTINE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "CO006", "루틴 타입을 찾을 수 없습니다."),
    ROUTINE_TYPE_NOT_ALLOWED_FOR_FOCUS(HttpStatus.BAD_REQUEST, "CO007", "집중 코스는 시작 시 루틴 타입을 지정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CourseErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
