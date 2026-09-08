-- member 테이블을 게스트 로그인 체계에 맞게 마이그레이션
--
-- 기존: id를 애플리케이션이 1L로 고정 발급(MemberSeeder), 배포 링크 접속자가 모두 공유하는 단일 계정
-- 변경: '게스트로 시작'을 누를 때마다 member 행을 생성 -> id AUTO_INCREMENT,
--       기기별 신원 식별용 guest_token(불투명 문자열) 컬럼 추가(NOT NULL, UNIQUE)
--
-- prod는 ddl-auto: validate라 배포 전 이 스크립트를 수동으로 한 번 실행해야 한다.
-- (local/dev는 ddl-auto: update지만 id의 AUTO_INCREMENT 전환은 자동 반영되지 않을 수 있어,
--  개발 DB는 member 테이블을 비우고 다시 만드는 편이 깔끔하다 - 데모 데이터라 부담 없음.)
--
-- 로그인 기능이 없던 프리런치 단계라 실사용자 데이터가 없다고 보지만, 기존 행이 있으면
-- guest_token을 먼저 채워야 NOT NULL / UNIQUE 제약을 걸 수 있다.
-- guest_token은 Authorization 헤더로 인증에 쓰이므로, 예측 가능한 UUID()(v1) 대신
-- 암호학적 난수인 RANDOM_BYTES()로 채운다.

ALTER TABLE member
    ADD COLUMN guest_token VARCHAR(36) NULL AFTER nickname;

UPDATE member
SET guest_token = LOWER(HEX(RANDOM_BYTES(16)))
WHERE guest_token IS NULL;

ALTER TABLE member
    MODIFY COLUMN guest_token VARCHAR(36) NOT NULL,
    ADD CONSTRAINT uk_member_guest_token UNIQUE (guest_token);

ALTER TABLE member
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
