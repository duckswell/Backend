package com.likelion.duckswell.domain.member.service;

import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.member.exception.MemberErrorCode;
import com.likelion.duckswell.domain.member.repository.MemberRepository;
import com.likelion.duckswell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getCurrentMember() {
        return memberRepository.findById(Member.DEFAULT_ID)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public void createDefaultMemberIfNotExists() {
        if (!memberRepository.existsById(Member.DEFAULT_ID)) {
            memberRepository.save(Member.createDefault());
        }
    }
}
