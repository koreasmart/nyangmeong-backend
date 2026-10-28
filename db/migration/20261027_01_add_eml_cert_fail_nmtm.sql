-- =====================================================================
-- 냥멍케어 MySQL ALTER  |  v0.9 위에 실행
-- EML_CERT.CERT_FAIL_NMTM 추가: 인증번호를 틀린 횟수. 제한 횟수에 닿으면 그 인증을 무효화해 6자리 번호 대입을 막는다
-- 근거: docs/api/member.md EmailCertificationVerifyRequest, ACC-001-05·06 (공통 인증 설정 app.email-cert.fail-limit)
-- 기존 행은 0으로 채워진다
-- =====================================================================

ALTER TABLE `EML_CERT`
  ADD COLUMN `CERT_FAIL_NMTM` decimal(2,0) NOT NULL DEFAULT '0' COMMENT '인증번호실패횟수' AFTER `CERT_NO_HASH`;
