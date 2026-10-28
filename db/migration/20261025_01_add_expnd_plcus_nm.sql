-- =====================================================================
-- 냥멍케어 MySQL ALTER  |  v0.9 위에 실행 (20261022_01 다음)
-- EXPND.EXPND_PLCUS_NM 추가: 지출 사용처(선택 입력)
-- 근거: 행안부 공통표준단어 사용처(PLCUS)·명(NM), AI매니저 화면설계서 SCR-AIM-009 ⑦ 가계부 매핑(사용처 = 병원명)
-- 길이는 검진기록 병원명(CKUP_RCD.HSPTL_NM varchar(80))과 같게 해 건강기록에서 넘어온 병원명이 잘리지 않게 한다. 기존 행은 NULL
-- 되돌리기: ALTER TABLE `EXPND` DROP COLUMN `EXPND_PLCUS_NM`;
-- =====================================================================

ALTER TABLE `EXPND`
  ADD COLUMN `EXPND_PLCUS_NM` varchar(80) NULL COMMENT '사용처명' AFTER `EXPND_AMT`;
