package com.likelion.duckswell.domain.demo.controller;

import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Demo", description = "심사/시연용 데모 데이터 리셋 API - 인증 없이 누구나 호출 가능")
public interface DemoResetApi {

    @Operation(
            summary = "데모 데이터를 고정 시나리오로 리셋",
            description = """
                    로그인이 없어 배포 링크에 접속한 누구나 같은 계정(member id=1)을 공유하기 때문에,
                    심사 기간 중 다른 참가자가 데이터를 건드려도 화면의 리셋 버튼으로 정해진 시연
                    시나리오(집중 코스 7일차, 시술 후 1~6일차 루틴 기록) 상태로 되돌리는 용도입니다.
                    member와 마스터 데이터(routine_type/ingredient/product 등)는 그대로 두고,
                    코스~체크리스트까지 회원이 실제로 앱을 쓰며 쌓는 데이터만 지운 뒤 다시 채웁니다.
                    """
    )
    ResponseEntity<ApiResponse<Void>> reset();
}
