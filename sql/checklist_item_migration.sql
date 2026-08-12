-- checklist_item 스키마를 최신 엔티티(ChecklistItem)에 맞게 마이그레이션
-- 기존 스키마: content_text(NOT NULL) 컬럼만 있고 course_id가 없음
-- 최신 엔티티: content_text 대신 title/description으로 분리, course_id 추가
-- prod는 ddl-auto: validate라 배포 전 이 스크립트를 수동으로 한 번 실행해야 함
--
-- 로그인 기능이 아직 없는 프리런치 단계라 실사용자 데이터가 없다고 보고,
-- 기존 checklist_item 행은 백필 없이 안전하게 삭제한다.

START TRANSACTION;

DELETE FROM checklist_item;

ALTER TABLE checklist_item
    DROP COLUMN content_text,
    ADD COLUMN course_id BIGINT NOT NULL AFTER member_id,
    ADD COLUMN title VARCHAR(50) NOT NULL AFTER item_date,
    ADD COLUMN description VARCHAR(200) NOT NULL AFTER title;

COMMIT;
