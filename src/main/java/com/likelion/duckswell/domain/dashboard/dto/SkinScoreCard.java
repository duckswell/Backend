package com.likelion.duckswell.domain.dashboard.dto;

public record SkinScoreCard(
        int current,
        int previous,
        int delta
) {
}
