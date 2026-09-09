package com.likelion.duckswell.domain.member;

import com.likelion.duckswell.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberSeeder implements ApplicationRunner {

    private final MemberService memberService;

    @Override
    public void run(ApplicationArguments args) {
        memberService.createDefaultMemberIfNotExists();
    }
}
