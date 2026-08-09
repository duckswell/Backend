package com.likelion.duckswell.domain.procedure.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProcedureRegisterRequest(
        @NotEmpty(message = "등록할 시술 정보가 최소 1개 이상이어야 합니다.")
        @Valid
        List<ProcedureItemRequest> procedures
) {
}
