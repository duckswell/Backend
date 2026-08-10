package com.likelion.duckswell.domain.diagnosis.client;

/** 외부 duckswellAI analyze.py의 {"redness_pct", "blemish_pct", "texture_pct"} 응답을 그대로 옮긴 값. */
public record CvScoreResult(double rednessPct, double blemishPct, double texturePct) {
}
