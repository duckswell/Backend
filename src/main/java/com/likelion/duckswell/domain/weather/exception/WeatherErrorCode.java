package com.likelion.duckswell.domain.weather.exception;

import com.likelion.duckswell.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum WeatherErrorCode implements ErrorCode {

    WEATHER_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "W001", "날씨 정보를 가져오지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    WeatherErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
