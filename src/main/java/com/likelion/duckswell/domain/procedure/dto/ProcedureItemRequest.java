package com.likelion.duckswell.domain.procedure.dto;

import com.likelion.duckswell.domain.procedure.entity.ProcedureAreaType;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public record ProcedureItemRequest(
        @Schema(description = "시술 종류 (SCALING=스케일링, PDT_PTT=PDT/PTT, EXTRACTION_INJECTION=압출/염증주사, IPL_LASER_TONING=IPL/레이저토닝)")
        @NotNull(message = "시술 종류는 필수입니다.")
        ProcedureType procedureType,

        @NotNull(message = "시술 일자는 필수입니다.")
        LocalDate procedureDate,

        @NotNull(message = "현재 회차는 필수입니다.")
        @Positive(message = "현재 회차는 1 이상이어야 합니다.")
        Integer currentCount,

        @NotNull(message = "총 회차는 필수입니다.")
        @Positive(message = "총 회차는 1 이상이어야 합니다.")
        Integer totalCount,

        @Schema(description = "시술 부위 (FULL_FACE=전체 얼굴, T_ZONE=T존, BUTTERFLY_ZONE=나비존, JAW=턱, CHEEK=볼)")
        @NotEmpty(message = "시술 부위는 최소 1개 이상이어야 합니다.")
        List<@NotNull(message = "시술 부위 값은 비어있을 수 없습니다.") ProcedureAreaType> areas
) {
    @AssertTrue(message = "현재 회차는 총 회차를 초과할 수 없습니다.")
    public boolean isCurrentCountValid() {
        return currentCount == null || totalCount == null || currentCount <= totalCount;
    }
}
