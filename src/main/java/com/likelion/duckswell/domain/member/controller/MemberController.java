package com.likelion.duckswell.domain.member.controller;

import com.likelion.duckswell.domain.member.dto.MemberResponse;
import com.likelion.duckswell.domain.member.service.MemberService;
import com.likelion.duckswell.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController implements MemberApi {

    private final MemberService memberService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(MemberResponse.from(memberService.getCurrentMember())));
    }
}
