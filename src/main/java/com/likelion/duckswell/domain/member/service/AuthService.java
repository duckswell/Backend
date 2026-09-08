package com.likelion.duckswell.domain.member.service;

import com.likelion.duckswell.domain.member.auth.GuestTokenExtractor;
import com.likelion.duckswell.domain.member.dto.GuestSessionResponse;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.member.repository.MemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;

    /**
     * 게스트 진입. 클라이언트는 앱을 켤 때마다 이 API를 호출하되, 저장해 둔 토큰이 있으면
     * Authorization 헤더에 실어 보낸다.
     * <ul>
     *   <li>토큰이 없으면: 새 게스트 계정을 만든다.</li>
     *   <li>유효한 토큰이면: 그 계정을 그대로 돌려준다(이어쓰기).</li>
     *   <li>서버가 모르는 토큰이면(예: DB 초기화 후): 새 게스트 계정을 만든다.</li>
     * </ul>
     */
    @Transactional
    public GuestSessionResponse startGuest(String authorizationHeader) {
        String token = GuestTokenExtractor.extract(authorizationHeader);
        if (token != null) {
            return memberRepository.findByGuestToken(token)
                    .map(GuestSessionResponse::from)
                    .orElseGet(this::createGuest);
        }
        return createGuest();
    }

    private GuestSessionResponse createGuest() {
        Member guest = memberRepository.save(Member.createGuest(UUID.randomUUID().toString()));
        return GuestSessionResponse.from(guest);
    }
}
