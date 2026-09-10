package com.likelion.duckswell.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨테이너 헬스체크(Dockerfile HEALTHCHECK)가 때리는 /api/auth/health가
 * GuestAuthInterceptor(/api/** 보호, /api/auth/** 만 제외)를 타지 않고
 * 인증 헤더 없이도 200을 돌려주는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 헬스체크는_인증_없이_200을_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk());
    }
}
