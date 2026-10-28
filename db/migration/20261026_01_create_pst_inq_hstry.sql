-- =====================================================================
-- 냥멍케어 MySQL CREATE  |  v0.9 위에 실행 (20261025_01 다음)
-- PST_INQ_HSTRY 추가: 게시글 조회 이력(같은 회원이 같은 글을 하루에 한 번만 조회수에 반영하기 위한 표)
-- 근거: 커뮤니티 기능명세서 FN-CMU-001-11 "조회수는 동일 사용자 1일 1회만 증가"
-- 사용: INSERT IGNORE로 (PST_SN, MBR_SN, INQ_YMD) 행이 새로 들어간 경우에만 PST.INQ_CNT를 1 올린다
-- 되돌리기: DROP TABLE `PST_INQ_HSTRY`;
-- =====================================================================

CREATE TABLE `PST_INQ_HSTRY` (
  `PST_INQ_HSTRY_SN` bigint NOT NULL AUTO_INCREMENT COMMENT '게시글조회이력일련번호',
  `PST_SN` bigint NOT NULL COMMENT '게시글일련번호',
  `MBR_SN` bigint NOT NULL COMMENT '회원일련번호',
  `INQ_YMD` date NOT NULL COMMENT '조회일자',
  `REG_DT` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  PRIMARY KEY (`PST_INQ_HSTRY_SN`),
  UNIQUE KEY `UK_PST_INQ_HSTRY_1` (`PST_SN`, `MBR_SN`, `INQ_YMD`),
  KEY `IDX_PST_INQ_HSTRY_1` (`MBR_SN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글조회이력';
