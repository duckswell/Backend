-- 상점(성분 추천) 기능 개발/테스트용 시드 데이터
-- 대상 DB: duckswell (로컬 MySQL, 각자 본인 환경에 한 번만 실행)
-- 실행 전: application-local.yml 기준 duckswell DB가 생성돼 있어야 하고,
--          앱을 한 번 bootRun 해서 Hibernate(ddl-auto=update)가 테이블을 만든 상태여야 함
-- 실행 순서: 1) 이 파일, 2) sql/products_seed.sql (실제 상품 데이터, ingredient가 있어야 하므로 반드시 이 다음)
--   mysql -u root -p --default-character-set=utf8mb4 -D duckswell < sql/seed_shop_demo.sql
--   mysql -u root -p --default-character-set=utf8mb4 -D duckswell < sql/products_seed.sql
-- member_id = 1 (MemberSeeder가 만드는 기본 회원) 기준

-- 1) routine_type 코드 테이블 (course.routine_type_code FK 대상)
INSERT INTO routine_type (code, name, description, icon_url) VALUES
  ('COOLDOWN', '쿨다운 케어', '시술 직후 열감과 붓기를 가라앉히는 루틴', NULL),
  ('CLEAR_UP', '트러블 케어', '트러블과 염증을 진정시키는 루틴', NULL),
  ('SEBUM_CONTROL', '피지 조절 케어', '과다 피지 분비를 조절하는 루틴', NULL),
  ('HYDRATION', '수분 보충 케어', '건조하고 당김이 있는 피부에 수분을 채우는 루틴', NULL);

-- 2) ingredient (확정 8개: VITAMIN 2 / MOISTURE 4 / PLANT_EXTRACT 2)
INSERT INTO ingredient (name, category, description, created_at, updated_at) VALUES
  ('비타민C', 'VITAMIN', '칙칙한 피부톤을 맑게 관리하고 외부 환경으로 인한 피부 산화를 방지하는 데 도움을 줘요', NOW(), NOW()),
  ('나이아신아마이드', 'VITAMIN', '칙칙한 피부톤과 눈에 띄는 잡티를 맑고 균일하게 관리하고 피지 조절, 피부 장벽 강화에 도움을 줘요', NOW(), NOW()),
  ('히알루론산', 'MOISTURE', '피부에 수분을 끌어당겨 건조함을 줄이고 촉촉하고 유연한 피부로 관리해 줘요', NOW(), NOW()),
  ('세라마이드', 'MOISTURE', '피부 장벽을 구성하는 성분으로, 수분이 빠져나가지 않도록 보호하고 보습을 유지해 줘요', NOW(), NOW()),
  ('판테놀', 'MOISTURE', '피부에 수분을 공급하고 외부 자극으로 약해진 피부 장벽을 편안하게 관리해 줘요', NOW(), NOW()),
  ('센텔라', 'PLANT_EXTRACT', '자극받아 붉어진 피부를 편안하게 진정하고 건강한 피부 컨디션을 유지하도록 도와줘요', NOW(), NOW()),
  ('알로에', 'PLANT_EXTRACT', '자극받은 피부를 산뜻하게 진정하고 건조해진 피부에 촉촉함을 더해줘요', NOW(), NOW()),
  ('징크 PCA', 'MOISTURE', '과도한 피지와 번들거림을 조절해 피부를 산뜻하고 깨끗한 상태로 유지하도록 도와줘요', NOW(), NOW());

SET @vitaminC = (SELECT id FROM ingredient WHERE name = '비타민C');
SET @niacinamide = (SELECT id FROM ingredient WHERE name = '나이아신아마이드');
SET @hyaluronic = (SELECT id FROM ingredient WHERE name = '히알루론산');
SET @ceramide = (SELECT id FROM ingredient WHERE name = '세라마이드');
SET @panthenol = (SELECT id FROM ingredient WHERE name = '판테놀');
SET @centella = (SELECT id FROM ingredient WHERE name = '센텔라');
SET @aloe = (SELECT id FROM ingredient WHERE name = '알로에');
SET @zincPca = (SELECT id FROM ingredient WHERE name = '징크 PCA');

-- 2-1) ingredient_tag (성분별 키워드)
INSERT INTO ingredient_tag (ingredient_id, tag) VALUES
  (@vitaminC, '피부톤 개선'), (@vitaminC, '항산화'), (@vitaminC, '잡티 관리'),
  (@niacinamide, '피부톤 개선'), (@niacinamide, '잡티 관리'), (@niacinamide, '장벽 강화'), (@niacinamide, '피지 조절'),
  (@hyaluronic, '수분 공급'), (@hyaluronic, '보습 유지'),
  (@ceramide, '장벽 강화'), (@ceramide, '수분 유지'), (@ceramide, '피부 보호'),
  (@panthenol, '보습'), (@panthenol, '장벽 강화'), (@panthenol, '진정'),
  (@centella, '진정'), (@centella, '붉은기'), (@centella, '피부 보호'),
  (@aloe, '진정'), (@aloe, '수분 공급'), (@aloe, '쿨링'),
  (@zincPca, '피지 조절'), (@zincPca, '번들거림 완화'), (@zincPca, '피부 청결');

-- 2-2) routine_type_ingredient (관리 타입별 고정 추천 성분 후보군 - LLM 성분 grounding용)
INSERT INTO routine_type_ingredient (routine_type_code, ingredient_id) VALUES
  ('COOLDOWN', @centella), ('COOLDOWN', @panthenol), ('COOLDOWN', @aloe),
  ('CLEAR_UP', @niacinamide), ('CLEAR_UP', @zincPca), ('CLEAR_UP', @centella),
  ('SEBUM_CONTROL', @niacinamide), ('SEBUM_CONTROL', @zincPca), ('SEBUM_CONTROL', @vitaminC),
  ('HYDRATION', @hyaluronic), ('HYDRATION', @ceramide), ('HYDRATION', @panthenol);

-- 3) product는 sql/products_seed.sql에서 실제 크롤링 데이터로 넣는다 (이 파일 다음에 실행)

-- 4) Course A: 진행중 코스 (member 1) — "진행중" 분기 테스트용
INSERT INTO course (member_id, procedure_id, course_type, routine_type_code, started_at, ended_at, status, created_at, updated_at)
VALUES (1, NULL, 'FOCUS', 'HYDRATION', CURDATE() - INTERVAL 4 DAY, NULL, 'IN_PROGRESS', NOW(), NOW());
SET @courseA = LAST_INSERT_ID();

-- Course A 루틴 5개 (최근 4일 전 ~ 오늘), 성분 카테고리 3종이 골고루 섞이도록 구성
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseA, CURDATE() - INTERVAL 4 DAY, NOW(), NOW());
SET @rA1 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseA, CURDATE() - INTERVAL 3 DAY, NOW(), NOW());
SET @rA2 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseA, CURDATE() - INTERVAL 2 DAY, NOW(), NOW());
SET @rA3 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseA, CURDATE() - INTERVAL 1 DAY, NOW(), NOW());
SET @rA4 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseA, CURDATE(), NOW(), NOW());
SET @rA5 = LAST_INSERT_ID();

INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA1, 1, '수분 토너', '화장솜에 적셔 결 따라 닦아냅니다.');
SET @sA1_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA1, 2, '장벽 크림', '적당량을 덜어 얼굴 전체에 펴 바릅니다.');
SET @sA1_2 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA2, 1, '진정 크림', '얇게 펴 바르고 흡수시킵니다.');
SET @sA2_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA3, 1, '진정 앰플', '자극 부위 위주로 두드려 흡수시킵니다.');
SET @sA3_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA3, 2, '보조 진정 케어', '가볍게 덧발라 줍니다.');
SET @sA3_2 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA4, 1, '피지 조절 세럼', '유분이 많은 부위 위주로 발라줍니다.');
SET @sA4_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA5, 1, '미백 앰플', '전체적으로 고르게 펴 바릅니다.');
SET @sA5_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rA5, 2, '트러블 보조 케어', '트러블 부위 위주로 발라줍니다.');
SET @sA5_2 = LAST_INSERT_ID();

-- 히알루론산 3회 / 판테놀 2회 / 세라마이드 2회 / 비타민C 1회 → 빈도수 top3(추천 성분 API) 검증용 분포
INSERT INTO routine_step_ingredient (routine_step_id, ingredient_id, ingredient_role) VALUES
  (@sA1_1, @hyaluronic, 'PRIMARY'),
  (@sA1_2, @panthenol, 'PRIMARY'),
  (@sA2_1, @hyaluronic, 'PRIMARY'),
  (@sA3_1, @ceramide, 'PRIMARY'),
  (@sA3_2, @panthenol, 'ALTERNATE'),
  (@sA4_1, @hyaluronic, 'PRIMARY'),
  (@sA5_1, @ceramide, 'PRIMARY'),
  (@sA5_2, @vitaminC, 'ALTERNATE');

-- 5) Course B: 완료된 과거 코스 (member 1) — "공백기 폴백" 분기 테스트용
INSERT INTO course (member_id, procedure_id, course_type, routine_type_code, started_at, ended_at, status, created_at, updated_at)
VALUES (1, NULL, 'DAILY', 'COOLDOWN', CURDATE() - INTERVAL 40 DAY, CURDATE() - INTERVAL 10 DAY, 'COMPLETED', NOW(), NOW());
SET @courseB = LAST_INSERT_ID();

INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseB, CURDATE() - INTERVAL 40 DAY, NOW(), NOW());
SET @rB1 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseB, CURDATE() - INTERVAL 30 DAY, NOW(), NOW());
SET @rB2 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseB, CURDATE() - INTERVAL 20 DAY, NOW(), NOW());
SET @rB3 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseB, CURDATE() - INTERVAL 15 DAY, NOW(), NOW());
SET @rB4 = LAST_INSERT_ID();
INSERT INTO routine (course_id, routine_date, created_at, updated_at) VALUES (@courseB, CURDATE() - INTERVAL 10 DAY, NOW(), NOW());
SET @rB5 = LAST_INSERT_ID();

INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB1, 1, '수분 토너', '화장솜에 적셔 결 따라 닦아냅니다.');
SET @sB1_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB2, 1, '장벽 크림', '적당량을 덜어 얼굴 전체에 펴 바릅니다.');
SET @sB2_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB3, 1, '미백 세럼', '전체적으로 고르게 펴 바릅니다.');
SET @sB3_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB4, 1, '진정 크림', '얇게 펴 바르고 흡수시킵니다.');
SET @sB4_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB5, 1, '트러블 케어 앰플', '트러블 부위 위주로 발라줍니다.');
SET @sB5_1 = LAST_INSERT_ID();
INSERT INTO routine_step (routine_id, step_order, step_name, method_text) VALUES (@rB5, 2, '보조 수분 케어', '가볍게 덧발라 줍니다.');
SET @sB5_2 = LAST_INSERT_ID();

-- 폴백 윈도우(최근 3개 루틴 = rB3~rB5)만 놓고 보면 센텔라 2회 / 알로에 1회 / 나이아신아마이드 1회
INSERT INTO routine_step_ingredient (routine_step_id, ingredient_id, ingredient_role) VALUES
  (@sB1_1, @hyaluronic, 'PRIMARY'),
  (@sB2_1, @ceramide, 'PRIMARY'),
  (@sB3_1, @centella, 'PRIMARY'),
  (@sB4_1, @centella, 'PRIMARY'),
  (@sB5_1, @aloe, 'PRIMARY'),
  (@sB5_2, @niacinamide, 'ALTERNATE');
