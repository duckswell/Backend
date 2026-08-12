-- procedure_record 스키마를 최신 엔티티(Procedure)에 맞게 마이그레이션
-- 기존 스키마: course_id 컬럼이 없어서 시술 정보가 코스와 연결되지 않고 회원 전체 이력으로만 관리됨
--             (집중 코스가 끝나도 이전 시술이 계속 "현재 시술"처럼 조회되는 문제가 있었음)
-- 최신 엔티티: course_id 추가 - 시술은 등록 시점에 진행 중이던 집중 코스에 귀속됨
-- prod는 ddl-auto: validate라 배포 전 이 스크립트를 수동으로 한 번 실행해야 함
--
-- 주의: MySQL에서 ALTER TABLE(DDL)은 실행 전 암시적 커밋을 수행하므로 DELETE와 ALTER를
-- 트랜잭션으로 묶어도 원자성이 보장되지 않는다(checklist_item_migration.sql과 동일한 이유).
-- 트랜잭션 대신 삭제 전 백업 테이블로 복구 가능하게 한다.
--
-- 로그인 기능이 아직 없는 프리런치 단계라 실사용자 데이터가 없다고 보지만, 만약을 대비해
-- procedure_record_backup_YYYYMMDD/procedure_area_backup_YYYYMMDD에 기존 데이터를 백업해 둔다.
-- 마이그레이션이 문제없이 끝난 걸 확인했으면 백업 테이블은 수동으로 지워도 된다.
-- (procedure_area가 procedure_record를 FK로 참조하므로 procedure_area를 먼저 지운다.)

CREATE TABLE procedure_record_backup_20260812 AS SELECT * FROM procedure_record;
CREATE TABLE procedure_area_backup_20260812 AS SELECT * FROM procedure_area;

DELETE FROM procedure_area;
DELETE FROM procedure_record;

ALTER TABLE procedure_record
    ADD COLUMN course_id BIGINT NOT NULL AFTER member_id;
