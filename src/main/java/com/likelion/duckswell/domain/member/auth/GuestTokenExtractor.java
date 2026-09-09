package com.likelion.duckswell.domain.member.auth;

import org.springframework.util.StringUtils;

/** {@code Authorization: Bearer <guestToken>} 헤더에서 토큰만 뽑아낸다. */
public final class GuestTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    private GuestTokenExtractor() {
    }

    /** 헤더가 없거나 형식이 맞지 않으면 null. */
    public static String extract(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
