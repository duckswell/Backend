-- checklist_item 스키마를 최신 엔티티(ChecklistItem)에 맞게 마이그레이션
-- 기존 스키마: content_text(NOT NULL) 컬럼만 있고 course_id가 없음
-- 최신 엔티티: content_text 대신 title/description으로 분리, course_id/item_order 추가,
--             (member_id, course_id, item_date, item_order) 유니크 제약 추가
--             (동시 요청으로 같은 체크리스트가 중복 생성되는 것을 DB 레벨에서 방지 -
--              item_order는 LLM 응답 문구와 무관한 배치 내 순번(0, 1)이라 title 기반보다 안전함)
-- prod는 ddl-auto: validate라 배포 전 이 스크립트를 수동으로 한 번 실행해야 함
--
-- 주의: MySQL에서 ALTER TABLE(DDL)은 실행 전 암시적 커밋을 수행하므로, 아래 DELETE와
-- ALTER를 하나의 트랜잭션(START TRANSACTION ~ COMMIT)으로 묶어도 원자성이 보장되지 않는다
-- (ALTER가 실패해도 DELETE는 이미 커밋된 상태로 남는다). 그래서 트랜잭션으로 묶는 대신,
-- 삭제 전에 백업 테이블을 만들어 복구 가능하게 한다.
--
-- 로그인 기능이 아직 없는 프리런치 단계라 실사용자 데이터가 없다고 보지만, 만약을 대비해
-- checklist_item_backup_YYYYMMDD 테이블에 기존 데이터를 백업해 둔다. 마이그레이션이 문제없이
-- 끝난 걸 확인했으면 백업 테이블은 수동으로 지워도 된다.

CREATE TABLE checklist_item_backup_20260812 AS SELECT * FROM checklist_item;

DELETE FROM checklist_item;

ALTER TABLE checklist_item
    DROP COLUMN content_text,
    ADD COLUMN course_id BIGINT NOT NULL AFTER member_id,
    ADD COLUMN item_order INT NOT NULL AFTER item_date,
    ADD COLUMN title VARCHAR(50) NOT NULL AFTER item_order,
    ADD COLUMN description VARCHAR(200) NOT NULL AFTER title,
    ADD CONSTRAINT uk_checklist_item_member_course_date_order
        UNIQUE (member_id, course_id, item_date, item_order);
