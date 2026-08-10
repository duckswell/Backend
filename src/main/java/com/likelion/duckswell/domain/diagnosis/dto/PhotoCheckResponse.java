package com.likelion.duckswell.domain.diagnosis.dto;

/** 품질 불량은 이 응답이 아니라 표준 에러 응답(CustomException)으로 내려간다 - 실패는 이 타입이 아예 안 나옴. */
public record PhotoCheckResponse(String photoId) {
}
