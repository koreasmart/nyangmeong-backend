-- =====================================================================
-- 냥멍케어 MySQL 알림 이동 경로 변경  |  v0.9 위에 실행 (20261026_01 다음)
-- INAPP_NOTI·CARE_MSN 의 MVMN_PATH 를 프론트 라우트(docs/routes.md COM-004-07)에 맞춘다
--   (1) 캐릭터 완성 알림 /mypage/pixel-characters → /mypage/characters
--   (2) 1:1 문의 답변 알림 /support/inquiries/{sn} → /inquiries/{sn}
--   (3) 시드 케어 일정 알림 /care → /health/calendar
--   (4) 시드 실종 만료 알림 /lost-reports → /community/missing
--   (5) 시드 공동양육 초대 결과 알림 /mypage/caregivers → /mypage/pets
--   (6) 시드 펫돌봄 알림 /chats → /chat
--   (7) 시드 커뮤니티 알림 /posts/{글번호} → /community (경로가 정해지면 다시 맞춘다)
--   (8) 시드 미션(CARE_MSN) /care, /care/calendar → /health/calendar
-- 몇 번을 실행해도 결과가 같다
-- 되돌리기(7번은 원래 글번호를 복원할 수 없어 되돌리지 못한다. 시드를 다시 만든다):
--   UPDATE `CARE_MSN` SET `MVMN_PATH`='/care' WHERE `MVMN_PATH`='/health/calendar' AND `MSN_NM`='케어리스트 완료하기';
--   UPDATE `CARE_MSN` SET `MVMN_PATH`='/care/calendar' WHERE `MVMN_PATH`='/health/calendar' AND `MSN_NM`='일정 등록하기';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/chats' WHERE `MVMN_PATH`='/chat';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/care' WHERE `MVMN_PATH`='/health/calendar';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/caregivers' WHERE `MVMN_PATH`='/mypage/pets';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/lost-reports' WHERE `MVMN_PATH`='/community/missing';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/pixel-characters' WHERE `MVMN_PATH`='/mypage/characters';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`=CONCAT('/support', `MVMN_PATH`) WHERE `MVMN_PATH` LIKE '/inquiries/%';
-- =====================================================================

UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/characters' WHERE `MVMN_PATH`='/mypage/pixel-characters';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`=REPLACE(`MVMN_PATH`, '/support/inquiries/', '/inquiries/') WHERE `MVMN_PATH` LIKE '/support/inquiries/%';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/health/calendar' WHERE `MVMN_PATH`='/care';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/community/missing' WHERE `MVMN_PATH`='/lost-reports';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/pets' WHERE `MVMN_PATH`='/mypage/caregivers';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/chat' WHERE `MVMN_PATH`='/chats';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/community' WHERE `MVMN_PATH` LIKE '/posts/%';
UPDATE `CARE_MSN` SET `MVMN_PATH`='/health/calendar' WHERE `MVMN_PATH` IN ('/care', '/care/calendar');
