package com.likelion.duckswell.domain.procedure.controller;

import com.likelion.duckswell.domain.procedure.dto.ProcedureRegisterRequest;
import com.likelion.duckswell.domain.procedure.dto.ProcedureResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Procedure", description = "시술 정보 등록 API")
public interface ProcedureApi {

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
