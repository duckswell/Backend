package com.likelion.duckswell.domain.member.controller;

import com.likelion.duckswell.domain.member.dto.MemberResponse;
import com.likelion.duckswell.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "고정 계정 정보 조회 API - 연동 필요 X")
public interface MemberApi {

    @Operation(
            summary = "현재 계정 정보 조회 - 테스트용",
            description = """
                    연동하지 않아도 되는 API입니다.
                    앱 시작 시 MemberSeeder가 고정 계정을 정상적으로 만들었는지 확인하는 디버깅/헬스체크 용도이며,
                    추후 계정 정보를 보여주는 화면이 생기면 그때 사용. 
                    """
    )
    ResponseEntity<ApiResponse<MemberResponse>> getMe();
}
