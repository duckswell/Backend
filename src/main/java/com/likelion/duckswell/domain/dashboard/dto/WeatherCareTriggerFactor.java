package com.likelion.duckswell.domain.dashboard.dto;

public enum WeatherCareTriggerFactor {

    ALL_GOOD(0, "모두 양호", "오늘은\n기본 케어를 편안하게 이어가세요"),
    UV_HIGH(1, "자외선 높음", "오늘은\n햇빛으로부터 피부를 보호해 주세요"),
    DUST_BAD(1, "미세먼지 나쁨", "오늘은\n외출 후 피부를 깨끗이 씻어 주세요"),
    UV_VERY_HIGH_OR_EXTREME(2, "자외선 매우높음·위험", "오늘은\n한낮의 야외 활동을 줄여 주세요"),
    HUMIDITY_VERY_LOW(2, "습도 매우낮음", "오늘은\n피부가 쉽게 건조해질 수 있어요"),
    HUMIDITY_VERY_HIGH(2, "습도 매우높음", "오늘은\n땀이 피부에 오래 남지 않게 해주세요"),
    DUST_VERY_BAD(2, "미세먼지 매우나쁨", "오늘은\n야외 활동을 가능한 줄여 주세요");

    private final int severity;
    private final String description;
    private final String summaryMessage;

    WeatherCareTriggerFactor(int severity, String description, String summaryMessage) {
        this.severity = severity;
        this.description = description;
        this.summaryMessage = summaryMessage;
    }

    public int getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }
}
