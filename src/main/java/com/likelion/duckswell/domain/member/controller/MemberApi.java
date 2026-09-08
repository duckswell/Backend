package com.likelion.duckswell.domain.member.controller;

import com.likelion.duckswell.domain.member.dto.MemberResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "현재 게스트 계정 정보 조회 API")
public interface MemberApi {

    @Operation(
            summary = "현재 게스트 계정 정보 조회",
            description = """
                    Authorization 헤더의 guestToken으로 특정되는 현재 게스트 계정 정보를 반환합니다.
                    """
    )
    ResponseEntity<ApiResponse<MemberResponse>> getMe();
}
