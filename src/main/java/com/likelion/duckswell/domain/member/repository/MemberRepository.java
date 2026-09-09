package com.likelion.duckswell.domain.member.repository;

import com.likelion.duckswell.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** 동시 요청 직렬화용 - 이 락을 잡고 있는 트랜잭션이 끝날 때까지 같은 회원 행을 잠그려는
     * 다른 트랜잭션은 대기한다(예: DemoResetService.reset()의 동시 실행 방지). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Member> findWithLockById(Long id);
}
