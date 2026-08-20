package com.likelion.duckswell.domain.demo.controller;

import com.likelion.duckswell.domain.demo.service.DemoResetService;
import com.likelion.duckswell.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoResetController implements DemoResetApi {

    private final DemoResetService demoResetService;

    @Override
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> reset() {
        demoResetService.reset();
        return ResponseEntity.ok(ApiResponse.success());
    }
}
