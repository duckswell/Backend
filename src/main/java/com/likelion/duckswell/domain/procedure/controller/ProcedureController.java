package com.likelion.duckswell.domain.procedure.controller;

import com.likelion.duckswell.domain.procedure.dto.ProcedureItemRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.domain.procedure.service.ProcedureService;
import com.likelion.duckswell.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
public class ProcedureController implements ProcedureApi {

    private final ProcedureService procedureService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProcedureResponse>>> getMyProcedures() {
        return ResponseEntity.ok(ApiResponse.success(procedureService.getMyProcedures()));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<List<ProcedureResponse>>> registerProcedures(@RequestBody @Valid ProcedureRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(procedureService.registerProcedures(request)));
    }

    @Override
    @PutMapping("/{procedureId}")
    public ResponseEntity<ApiResponse<ProcedureResponse>> updateProcedure(
            @PathVariable Long procedureId,
            @RequestBody @Valid ProcedureItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(procedureService.updateProcedure(procedureId, request)));
    }

    @Override
    @DeleteMapping("/{procedureId}")
    public ResponseEntity<ApiResponse<Void>> deleteProcedure(@PathVariable Long procedureId) {
        procedureService.deleteProcedure(procedureId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
