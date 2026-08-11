package com.likelion.duckswell.domain.procedure.dto;

import com.likelion.duckswell.domain.procedure.entity.ProcedureAreaType;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public record ProcedureItemRequest(
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

        @NotEmpty(message = "시술 부위는 최소 1개 이상이어야 합니다.")
        List<@NotNull(message = "시술 부위 값은 비어있을 수 없습니다.") ProcedureAreaType> areas
) {
    @AssertTrue(message = "현재 회차는 총 회차를 초과할 수 없습니다.")
    public boolean isCurrentCountValid() {
        return currentCount == null || totalCount == null || currentCount <= totalCount;
    }
}
