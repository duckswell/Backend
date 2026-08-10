package com.likelion.duckswell.domain.procedure.controller;

import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Procedure", description = "시술 정보 등록/조회/수정/삭제 API")
public interface ProcedureApi {

    @Operation(
            summary = "내 시술 정보 전체 조회",
            description = """
                    현재 회원이 등록한 시술 정보 전체 목록을 조회합니다. 별도의 상세 조회 없이
                    목록 응답에 종류/일자/횟수/부위 등 모든 필드를 그대로 포함합니다.
                    """
    )
    ResponseEntity<ApiResponse<List<ProcedureResponse>>> getMyProcedures();

    @Operation(
            summary = "시술 정보 등록",
            description = """
                    시술 정보(종류/일자/횟수/부위)를 한 번에 여러 건 등록합니다.
                    시술 등록 화면에서 "시술 추가"로 늘어난 폼 목록을 한 번의 요청으로
                    함께 등록하는 것을 전제로 합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "시술 정보 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (필수값 누락, 시술 부위 미지정 등)")
    })
    ResponseEntity<ApiResponse<List<ProcedureResponse>>> registerProcedures(ProcedureRegisterRequest request);
}
