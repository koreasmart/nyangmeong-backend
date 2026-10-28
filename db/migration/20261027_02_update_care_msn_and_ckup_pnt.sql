-- =====================================================================
-- 냥멍케어 MySQL 케어미션 마스터·포인트 거래유형 변경  |  v0.9 위에 실행 (20261027_01 다음)
-- CARE_MSN을 마이페이지 기능명세서 FN-MYP-020-03·05와 건강기록 FN-HLT-003-12에 맞춘다
--   (1) 시드 미션 이름을 명세 이름으로 바꾼다 (과거 DAY_MSN이 FK로 참조하므로 행은 지우지 않음)
--   (2) 미션 21개를 풀·배점·횟수까지 맞춘다. 없는 미션(케어리스트 기본 항목 11개, 관찰 기록)은 새로 넣는다
--       일상케어(MYPC0010) 9개: 매일 할 수 있는 것만. 하루 8개 중 5개
--       활동기반(MYPC0011) 12개: 정기관리 기본 항목 6개, 산책(강아지만), 관찰 기록(관찰 중일 때), 펫이야기(대표만) 포함. 하루 3개
--       달성 기준 횟수는 모두 1회, 배점은 1~20P (FN-MYP-020-05)
--   (3) 대응하는 미션이 없는 '케어리스트 완료하기'는 USE_YN='N'
--   (4) MYPG03에 MYPC0021 '검진기록 등록' 추가 (FN-MYP-020-08)
-- 기존 행의 MVMN_PATH는 그대로 두고, 새로 넣는 행만 경로를 준다
-- 되돌리기:
--   DELETE FROM `COM_CD` WHERE `COM_CD`='MYPC0021';
--   UPDATE `CARE_MSN` SET `USE_YN`='Y' WHERE `MSN_NM`='케어리스트 완료하기';
--   DELETE FROM `CARE_MSN` WHERE `MSN_NM` IN ('화장실 청소하기','양치하기','빗질하기','발 관리하기','놀이하기','귀 청소하기','발톱 관리하기','목욕하기','눈 주변 관리하기','발바닥 털 관리하기','구강 상태 확인하기','관찰 기록 추가하기');
--   UPDATE `CARE_MSN` SET `MSN_NM`='배변 상태 기록하기', `CMPN_PNT`=10 WHERE `MSN_NM`='배변 기록하기';
--   UPDATE `CARE_MSN` SET `MSN_NM`='커뮤니티 글 쓰기', `CMPN_PNT`=30 WHERE `MSN_NM`='펫이야기 글 등록하기';
--   UPDATE `CARE_MSN` SET `MSN_NM`='댓글 3개 남기기', `CMPN_PNT`=15, `ACHV_CRTR_NMTM`=3 WHERE `MSN_NM`='펫이야기 댓글 작성하기';
--   UPDATE `CARE_MSN` SET `MSN_NM`='가계부 기록하기', `CMPN_PNT`=15 WHERE `MSN_NM`='가계부 지출 등록하기';
--   UPDATE `CARE_MSN` SET `MSN_NM`='일정 등록하기', `CMPN_PNT`=15 WHERE `MSN_NM`='케어 캘린더 일정 등록하기';
--   UPDATE `CARE_MSN` SET `CMPN_PNT`=20 WHERE `MSN_NM` IN ('체중 기록하기','산책 기록하기');
--   (위 DELETE는 새 미션이 DAY_MSN에 출제되기 전에만 가능)
-- =====================================================================

UPDATE `CARE_MSN` SET `MSN_NM`='배변 기록하기' WHERE `MSN_NM`='배변 상태 기록하기';
UPDATE `CARE_MSN` SET `MSN_NM`='펫이야기 글 등록하기' WHERE `MSN_NM`='커뮤니티 글 쓰기';
UPDATE `CARE_MSN` SET `MSN_NM`='펫이야기 댓글 작성하기' WHERE `MSN_NM`='댓글 3개 남기기';
UPDATE `CARE_MSN` SET `MSN_NM`='가계부 지출 등록하기' WHERE `MSN_NM`='가계부 기록하기';
UPDATE `CARE_MSN` SET `MSN_NM`='케어 캘린더 일정 등록하기' WHERE `MSN_NM`='일정 등록하기';
UPDATE `CARE_MSN` SET `USE_YN`='N', `MDFR_ID`='SYSTEM' WHERE `MSN_NM`='케어리스트 완료하기';

INSERT INTO `CARE_MSN` (`MSN_POOL_CD`,`MSN_NM`,`CMPN_PNT`,`ACHV_CRTR_NMTM`,`MVMN_PATH`) VALUES
  ('MYPC0010','화장실 청소하기',10,1,'/health/calendar'),
  ('MYPC0010','양치하기',5,1,'/health/calendar'),
  ('MYPC0010','빗질하기',5,1,'/health/calendar'),
  ('MYPC0010','발 관리하기',3,1,'/health/calendar'),
  ('MYPC0010','놀이하기',5,1,'/health/calendar'),
  ('MYPC0010','체중 기록하기',10,1,'/health'),
  ('MYPC0010','식사량 기록하기',10,1,'/health'),
  ('MYPC0010','음수량 기록하기',10,1,'/health'),
  ('MYPC0010','배변 기록하기',5,1,'/health'),
  ('MYPC0011','가계부 지출 등록하기',5,1,'/mypage/expenses'),
  ('MYPC0011','케어 캘린더 일정 등록하기',5,1,'/health/calendar'),
  ('MYPC0011','귀 청소하기',10,1,'/health/calendar'),
  ('MYPC0011','발톱 관리하기',15,1,'/health/calendar'),
  ('MYPC0011','목욕하기',20,1,'/health/calendar'),
  ('MYPC0011','눈 주변 관리하기',5,1,'/health/calendar'),
  ('MYPC0011','발바닥 털 관리하기',10,1,'/health/calendar'),
  ('MYPC0011','구강 상태 확인하기',5,1,'/health/calendar'),
  ('MYPC0011','산책 기록하기',10,1,'/health'),
  ('MYPC0011','관찰 기록 추가하기',10,1,'/health'),
  ('MYPC0011','펫이야기 글 등록하기',20,1,'/community'),
  ('MYPC0011','펫이야기 댓글 작성하기',5,1,'/community') AS N
ON DUPLICATE KEY UPDATE
  `MSN_POOL_CD`=N.`MSN_POOL_CD`,
  `CMPN_PNT`=N.`CMPN_PNT`,
  `ACHV_CRTR_NMTM`=N.`ACHV_CRTR_NMTM`,
  `USE_YN`='Y',
  `MDFR_ID`='SYSTEM';

INSERT INTO `COM_CD` (`COM_CD_PRNT_SN`,`COM_CD`,`COM_CD_NM`,`COM_CD_EXPLN`,`SORT_SEQ`) VALUES ((SELECT p.`COM_CD_SN` FROM (SELECT `COM_CD_SN` FROM `COM_CD` WHERE `COM_CD`='MYPG03') p),'MYPC0021','검진기록 등록','적립(+). 반려동물·검진일당 1회 20P, 등록일 기준 최근 7일 이내 검진일만 (FN-MYP-020-08)',15);
