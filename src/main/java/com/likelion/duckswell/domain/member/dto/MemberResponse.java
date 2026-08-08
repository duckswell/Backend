package com.likelion.duckswell.domain.member.dto;

import com.likelion.duckswell.domain.member.entity.Member;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String nickname,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getNickname(), member.getCreatedAt());
    }
}
