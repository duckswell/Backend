package com.likelion.duckswell.domain.dashboard.dto;

public enum WeatherCareTriggerFactor {

    UV_HIGH("자외선 높음", "오늘은 햇빛으로부터 피부를 보호해 주세요"),
    UV_VERY_HIGH_OR_EXTREME("자외선 매우높음·위험", "오늘은 한낮의 야외 활동을 줄여 주세요"),
    HUMIDITY_VERY_LOW("습도 매우낮음", "오늘은 피부가 쉽게 건조해질 수 있어요"),
    HUMIDITY_VERY_HIGH("습도 매우높음", "오늘은 땀이 피부에 오래 남지 않게 해주세요"),
    DUST_BAD("미세먼지 나쁨", "오늘은 외출 후 피부를 깨끗이 씻어 주세요"),
    DUST_VERY_BAD("미세먼지 매우나쁨", "오늘은 야외 활동을 가능한 줄여 주세요"),
    ALL_GOOD("모두 양호", "오늘은 기본 케어를 편안하게 이어가세요");

    private final String description;
    private final String summaryMessage;

    WeatherCareTriggerFactor(String description, String summaryMessage) {
        this.description = description;
        this.summaryMessage = summaryMessage;
    }

    public String getDescription() {
        return description;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }
}
