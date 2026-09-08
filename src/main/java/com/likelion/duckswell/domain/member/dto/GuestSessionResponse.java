package com.likelion.duckswell.domain.member.dto;

import com.likelion.duckswell.domain.member.entity.Member;

public record GuestSessionResponse(
        Long memberId,
        String guestToken,
        String nickname
) {
    public static GuestSessionResponse from(Member member) {
        return new GuestSessionResponse(member.getId(), member.getGuestToken(), member.getNickname());
    }
}
