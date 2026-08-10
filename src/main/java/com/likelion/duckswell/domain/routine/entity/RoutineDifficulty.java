package com.likelion.duckswell.domain.routine.entity;

import lombok.Getter;

/** title은 난이도별로 고정된 값 - AI가 매번 다르게 생성하지 않는다. subtitle은 AI가 매번 생성. */
@Getter
public enum RoutineDifficulty {
    LIGHT("가벼운 관리"),
    BASIC("기본 관리"),
    INTENSIVE("꼼꼼한 관리");

    private final String title;

    RoutineDifficulty(String title) {
        this.title = title;
    }
}
