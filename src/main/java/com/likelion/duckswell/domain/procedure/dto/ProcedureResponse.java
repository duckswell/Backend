package com.likelion.duckswell.domain.procedure.dto;

import com.likelion.duckswell.domain.procedure.entity.Procedure;
import com.likelion.duckswell.domain.procedure.entity.ProcedureArea;
import com.likelion.duckswell.domain.procedure.entity.ProcedureAreaType;
import com.likelion.duckswell.domain.procedure.entity.ProcedureType;
import java.time.LocalDate;
import java.util.List;

public record ProcedureResponse(
        Long id,
        ProcedureType procedureType,
        String procedureTypeName,
        LocalDate procedureDate,
        Integer currentCount,
        Integer totalCount,
        List<ProcedureAreaType> areas
) {
    public static ProcedureResponse from(Procedure procedure) {
        return new ProcedureResponse(
                procedure.getId(),
                procedure.getProcedureType(),
                procedure.getProcedureType().getDisplayName(),
                procedure.getProcedureDate(),
                procedure.getCurrentCount(),
                procedure.getTotalCount(),
                procedure.getAreas().stream().map(ProcedureArea::getArea).toList()
        );
    }
}
