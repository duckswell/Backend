package com.likelion.duckswell.domain.member.repository;

import com.likelion.duckswell.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
