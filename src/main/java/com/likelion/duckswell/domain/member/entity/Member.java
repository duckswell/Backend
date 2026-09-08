package com.likelion.duckswell.domain.member.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private static final String GUEST_NICKNAME = "게스트";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    /**
     * 게스트마다 발급되는 불투명 식별자. 기기에 저장했다가 요청 헤더로 되돌려 보내면
     * 서버가 이 값으로 회원을 특정한다. 이 토큰이 곧 신원.
     */
    @Column(name = "guest_token", nullable = false, unique = true, length = 36)
    private String guestToken;

    private Member(String nickname, String guestToken) {
        this.nickname = nickname;
        this.guestToken = guestToken;
    }

    public static Member createGuest(String guestToken) {
        return new Member(GUEST_NICKNAME, guestToken);
    }
}
