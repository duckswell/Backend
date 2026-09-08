package com.likelion.duckswell.domain.member.auth;

import com.likelion.duckswell.domain.member.exception.MemberErrorCode;
import com.likelion.duckswell.global.exception.CustomException;

/**
 * 현재 요청을 보낸 게스트의 memberId를 요청 처리 스레드에 담아두는 홀더.
 *
 * <p>Spring Security의 {@code SecurityContextHolder}와 같은 방식이다. 모든 서비스 메서드에
 * memberId 파라미터를 줄줄이 넘기는 대신, {@code GuestAuthInterceptor}가 요청 진입 시 한 번
 * 채워두고 각 서비스가 필요할 때 꺼내 쓴다. 요청이 끝나면 인터셉터가 {@link #clear()}로 비운다.
 */
public final class CurrentMemberContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private CurrentMemberContext() {
    }

    public static void set(Long memberId) {
        HOLDER.set(memberId);
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 인증된 게스트가 없으면 null. 인증 여부를 판단할 때만 쓴다. */
    public static Long getMemberIdOrNull() {
        return HOLDER.get();
    }

    /** 인증된 게스트의 memberId. 없으면 401(UNAUTHENTICATED). */
    public static Long getMemberId() {
        Long memberId = HOLDER.get();
        if (memberId == null) {
            throw new CustomException(MemberErrorCode.UNAUTHENTICATED);
        }
        return memberId;
    }
}
