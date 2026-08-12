package com.likelion.duckswell.domain.dashboard.dto;

public record RecoveryBannerResponse(
        SkinScoreCard redness,
        SkinScoreCard texture,
        SkinScoreCard blemish,
        String summaryMessage
) {
}
