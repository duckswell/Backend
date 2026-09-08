package com.likelion.duckswell.domain.member.auth;

import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.member.exception.MemberErrorCode;
import com.likelion.duckswell.domain.member.repository.MemberRepository;
import com.likelion.duckswell.global.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 모든 보호 대상 요청에서 {@code Authorization: Bearer <guestToken>} 헤더를 읽어 현재 게스트를
 * 특정하고 {@link CurrentMemberContext}에 담는다. 토큰이 없으면 401(UNAUTHENTICATED),
 * 토큰이 있으나 매칭되는 회원이 없으면 401(INVALID_GUEST_TOKEN).
 */
@Component
@RequiredArgsConstructor
public class GuestAuthInterceptor implements HandlerInterceptor {

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = GuestTokenExtractor.extract(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            throw new CustomException(MemberErrorCode.UNAUTHENTICATED);
        }

        Member member = memberRepository.findByGuestToken(token)
                .orElseThrow(() -> new CustomException(MemberErrorCode.INVALID_GUEST_TOKEN));
        CurrentMemberContext.set(member.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentMemberContext.clear();
    }
}
