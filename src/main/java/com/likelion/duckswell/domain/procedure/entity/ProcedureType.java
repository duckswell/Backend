package com.likelion.duckswell.domain.procedure.entity;

import lombok.Getter;

@Getter
public enum ProcedureType {

    SCALING("스케일링"),
    PDT_PTT("PDT/PTT"),
    EXTRACTION_INJECTION("압출/염증주사"),
    IPL_LASER_TONING("IPL/레이저토닝");

    private final String displayName;

    ProcedureType(String displayName) {
        this.displayName = displayName;
    }
}
