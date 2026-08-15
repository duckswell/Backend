package com.likelion.duckswell.domain.dashboard.dto;

/** 기획 표의 회복 타임라인(초기 1~3일차/중기 4~6일차/후기 6~7일 이후) 기준 - 6일차 겹침은 후기로 처리. */
public enum RecoveryStage {

    EARLY("시술 후 %d일째,\n피부 진정에 집중할 때예요"),
    MID("시술 후 %d일째,\n피부 장벽을 채워줄 시기예요"),
    LATE("시술 후 %d일째,\n흔적 개선을 시작해도 좋은 시기예요");

    private final String messageFormat;

    RecoveryStage(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public static RecoveryStage from(int dayNumber) {
        if (dayNumber <= 3) {
            return EARLY;
        }
        if (dayNumber <= 5) {
            return MID;
        }
        return LATE;
    }

    public String buildMessage(int dayNumber) {
        return messageFormat.formatted(dayNumber);
    }
}
