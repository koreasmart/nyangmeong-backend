-- =====================================================================
-- 냥멍케어 MySQL ALTER  |  v0.9 위에 실행
-- EXPND.IDMPT_DMND_ID 추가: 지출 등록(POST /expenses) 중복 클릭·재전송 차단
-- 근거: docs/convention/api.md 멱등 규칙, docs/api/expense.md ExpenseLineRequest.idmptDmndId
-- 한 요청이 여러 줄을 만들므로 줄마다 다른 값을 받는다. 기존 행·영수증 저장 등 값이 없는 행은 NULL(유일 키에서 NULL 은 중복 허용)
-- =====================================================================

ALTER TABLE `EXPND`
  ADD COLUMN `IDMPT_DMND_ID` varchar(100) NULL COMMENT '멱등요청ID' AFTER `EXPND_YMD`,
  ADD UNIQUE KEY `UK_EXPND_1` (`IDMPT_DMND_ID`);
