package com.likelion.duckswell.domain.member.controller;

import com.likelion.duckswell.domain.member.dto.GuestSessionResponse;
import com.likelion.duckswell.domain.member.service.AuthService;
import com.likelion.duckswell.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<GuestSessionResponse>> startGuest(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(authService.startGuest(authorizationHeader)));
    }
}
