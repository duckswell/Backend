package com.likelion.duckswell.global.exception;

import com.likelion.duckswell.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException: {}", errorCode.getMessage(), e);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());
        log.warn("MethodArgumentNotValidException: {}", message);
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE.getCode(), CommonErrorCode.INVALID_INPUT_VALUE.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
