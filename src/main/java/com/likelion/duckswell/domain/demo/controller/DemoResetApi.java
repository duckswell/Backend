package com.likelion.duckswell.domain.demo.controller;

import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Demo", description = "심사/시연용 데모 데이터 리셋 API")
public interface DemoResetApi {

    @Operation(
            summary = "현재 게스트의 데모 데이터를 고정 시나리오로 리셋",
            description = """
                    심사위원이 각자 자기 기기로 접속해 앱을 조작해 본 뒤, 화면의 리셋 버튼으로
                    정해진 시연 시나리오(집중 코스 7일차, 시술 후 1~6일차 루틴 기록) 상태로
                    되돌리는 용도입니다.
                    현재 게스트 계정과 마스터 데이터(routine_type/ingredient/product 등)는 그대로 두고,
                    그 게스트가 코스~체크리스트까지 앱을 쓰며 쌓은 데이터만 지운 뒤 다시 채웁니다.
                    """
    )
    ResponseEntity<ApiResponse<Void>> reset();
}
