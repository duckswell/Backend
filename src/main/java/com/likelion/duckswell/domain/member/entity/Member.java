package com.likelion.duckswell.domain.member.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    // 로그인 없이 미리 만든 계정 하나만 사용 예정.
    // auto-increment 대신 고정 PK를 직접 부여.
    public static final Long DEFAULT_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private String nickname;

    private Member(Long id, String nickname) {
        this.id = id;
        this.nickname = nickname;
    }

    public static Member createDefault() {
        return new Member(DEFAULT_ID, "테스트 계정");
    }
}
