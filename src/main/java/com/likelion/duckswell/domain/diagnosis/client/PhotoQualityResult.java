package com.likelion.duckswell.domain.diagnosis.client;

/** 외부 duckswellAI check_photo.py의 {"ok": bool, "reason": str|null} 응답을 그대로 옮긴 값. */
public record PhotoQualityResult(boolean ok, String reason) {
}
