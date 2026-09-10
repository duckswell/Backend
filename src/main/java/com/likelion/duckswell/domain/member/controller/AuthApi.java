package com.likelion.duckswell.domain.member.controller;

import com.likelion.duckswell.domain.member.dto.GuestSessionResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "게스트 로그인 API")
public interface AuthApi {

    @SecurityRequirements
    @Operation(
            summary = "게스트로 시작 / 이어쓰기",
            description = """
                    로고 화면의 '게스트로 시작' 버튼이 호출하면 됩니다. 앱을 켤 때마다 이 API를
                    호출하되, 이전에 저장해 둔 guestToken이 있으면 `Authorization: Bearer <guestToken>` 헤더에 실어 보내세요.

                    - 토큰 없이 호출: 새 게스트 계정을 만들어 memberId·guestToken·nickname 반환
                    - 유효한 토큰으로 호출: 그 계정을 그대로 반환(이어쓰기)
                    - 서버가 모르는 토큰으로 호출(예: DB 초기화): 새 게스트 계정 생성

                    응답의 guestToken을 항상 저장하고, 이후 모든 요청에
                    `Authorization: Bearer <guestToken>` 헤더로 실어 보내면 됩니다.
                    """
    )
    ResponseEntity<ApiResponse<GuestSessionResponse>> startGuest(String authorizationHeader);

    @SecurityRequirements
    @Operation(
            summary = "헬스체크",
            description = """
                    컨테이너 헬스체크 전용 엔드포인트입니다. 인증 없이 항상 200을 반환하며,
                    애플리케이션이 요청을 처리할 수 있는 상태인지만 확인합니다.
                    """
    )
    ResponseEntity<Void> health();
}
