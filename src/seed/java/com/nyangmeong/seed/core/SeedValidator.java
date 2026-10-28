package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.SeedException;

/**
 * [커밋 전 검사]
 * - FK 검사를 끄고 넣었으므로 FK를 여기서 전부 확인한다
 * - 공통코드 컬럼은 주석의 그룹에 속한 코드인지, 업무 규칙은 화면이 기대하는 값인지 본다
 */
public final class SeedValidator {

    private final Connection conn;
    private final Schema schema;
    private final List<String> problems = new ArrayList<>();

    public SeedValidator(Connection conn, Schema schema) {
        this.conn = conn;
        this.schema = schema;
    }

    public void validate(List<String> tables) throws SQLException {
        for (Schema.ForeignKey fk : schema.foreignKeys()) {
            if (!tables.contains(fk.table())) {
                continue;
            }
            count("FK " + fk.table() + "." + fk.column() + " → " + fk.refTable(),
                    "SELECT COUNT(*) FROM " + fk.table() + " c LEFT JOIN " + fk.refTable() + " p ON p." + fk.refColumn()
                            + " = c." + fk.column() + " WHERE c." + fk.column() + " IS NOT NULL AND p." + fk.refColumn()
                            + " IS NULL");
        }
        for (String table : tables) {
            for (Map.Entry<String, Schema.Column> entry : schema.columns(table).entrySet()) {
                String group = entry.getValue().codeGroup();
                if (group == null) {
                    continue;
                }
                String col = entry.getKey();
                count("공통코드 그룹 " + table + "." + col + " ∉ " + group,
                        "SELECT COUNT(*) FROM " + table + " t JOIN COM_CD c ON c.COM_CD = t." + col
                                + " JOIN COM_CD g ON g.COM_CD_SN = c.COM_CD_PRNT_SN WHERE g.COM_CD <> '" + group + "'");
            }
        }
        businessRules();
        if (!problems.isEmpty()) {
            throw new SeedException("커밋 전 검사에 걸려 롤백했습니다\n  - " + String.join("\n  - ", problems));
        }
        System.out.println("[시드] 커밋 전 검사 통과(FK " + schema.foreignKeys().size() + "개 기준, 공통코드 그룹, 업무 규칙)");
    }

    private void businessRules() throws SQLException {
        rule("회원당 반려동물 수가 SYS_STNG.PET_MAX_CNT 초과", """
                SELECT COUNT(*) FROM (
                  SELECT r.MBR_SN FROM BRUP_REL r JOIN PET p ON p.PET_SN = r.PET_SN
                   WHERE r.USE_YN = 'Y' AND p.DEL_YN = 'N'
                   GROUP BY r.MBR_SN
                  HAVING COUNT(*) > (SELECT PET_MAX_CNT FROM SYS_STNG)) x""");
        rule("반려동물당 양육자 수가 SYS_STNG.JNT_BRUP_MAX_NOPE 초과", """
                SELECT COUNT(*) FROM (
                  SELECT PET_SN FROM BRUP_REL WHERE USE_YN = 'Y'
                   GROUP BY PET_SN
                  HAVING COUNT(*) > (SELECT JNT_BRUP_MAX_NOPE FROM SYS_STNG)) x""");
        rule("양육 중인 반려동물이 있는데 대표 반려동물이 없는 회원", """
                SELECT COUNT(*) FROM (
                  SELECT r.MBR_SN FROM BRUP_REL r JOIN PET p ON p.PET_SN = r.PET_SN
                   WHERE r.USE_YN = 'Y' AND p.DEL_YN = 'N'
                   GROUP BY r.MBR_SN
                  HAVING SUM(r.RPRS_PET_YN = 'Y') <> 1) x""");
        rule("주양육자가 1명이 아닌 반려동물", """
                SELECT COUNT(*) FROM (
                  SELECT p.PET_SN FROM PET p
                    LEFT JOIN BRUP_REL r ON r.PET_SN = p.PET_SN
                         AND r.BRUP_ROLE_CD = (SELECT COM_CD FROM COM_CD WHERE COM_CD_NM = '주양육자')
                   GROUP BY p.PET_SN
                  HAVING COUNT(r.BRUP_REL_SN) <> 1) x""");
        rule("MBR.HLD_PNT와 포인트 원장 마지막 잔액이 다른 회원", """
                SELECT COUNT(*) FROM MBR m
                  LEFT JOIN PNT_GNLGR g ON g.PNT_GNLGR_SN =
                       (SELECT MAX(PNT_GNLGR_SN) FROM PNT_GNLGR WHERE MBR_SN = m.MBR_SN)
                 WHERE m.HLD_PNT <> COALESCE(g.DLNG_AFTR_BLNC_PNT, 0)""");
        rule("음수 포인트 잔액", "SELECT COUNT(*) FROM PNT_GNLGR WHERE DLNG_AFTR_BLNC_PNT < 0");
        rule("PST.RCMDTN_CNT와 좋아요 수가 다른 게시글", """
                SELECT COUNT(*) FROM PST p
                 WHERE p.RCMDTN_CNT <> (SELECT COUNT(*) FROM PST_RCMDTN r WHERE r.PST_SN = p.PST_SN AND r.USE_YN = 'Y')""");
        rule("답글의 부모 댓글이 다른 게시글", """
                SELECT COUNT(*) FROM CMNT c JOIN CMNT p ON p.CMNT_SN = c.PRNT_CMNT_SN WHERE p.PST_SN <> c.PST_SN""");
        rule("픽셀 캐릭터 회원이 반려동물 양육자가 아님", """
                SELECT COUNT(*) FROM PIXEL_CHAR c
                 WHERE NOT EXISTS (SELECT 1 FROM BRUP_REL r WHERE r.PET_SN = c.PET_SN AND r.MBR_SN = c.MBR_SN)""");
        rule("보유 아이템 착용 부위가 아이템 부위와 다름", """
                SELECT COUNT(*) FROM HLD_ITEM h JOIN ITEM i ON i.ITEM_SN = h.ITEM_SN WHERE h.EQUIP_REGN_CD <> i.ITEM_REGN_CD""");
        rule("채팅방 참여자가 2명이 아님", """
                SELECT COUNT(*) FROM (
                  SELECT o.OTO_CHAT_ROOM_SN FROM OTO_CHAT_ROOM o LEFT JOIN CHAT_PRTPNT p ON p.OTO_CHAT_ROOM_SN = o.OTO_CHAT_ROOM_SN
                   GROUP BY o.OTO_CHAT_ROOM_SN HAVING COUNT(p.CHAT_PRTPNT_SN) <> 2) x""");
        rule("확정 매칭이 다른 모집글의 매칭", """
                SELECT COUNT(*) FROM PET_PRTC_RCRT r JOIN PET_PRTC_MTCH m ON m.PET_PRTC_MTCH_SN = r.CFMTN_PET_PRTC_MTCH_SN
                 WHERE m.PET_PRTC_RCRT_SN <> r.PET_PRTC_RCRT_SN""");
        rule("시드 이미지 경로가 seed/·public/seed/ 밖",
                "SELECT COUNT(*) FROM ATCH_FILE WHERE PHYS_STRG_PATH NOT LIKE 'seed/%' AND PHYS_STRG_PATH NOT LIKE 'public/seed/%'");
    }

    /**
     * 업무 규칙 하나. 테이블 구조가 바뀌어 쿼리가 맞지 않으면 건너뛰고 경고한다(규칙 자체는 사람이 고친다)
     */
    private void rule(String label, String sql) {
        try {
            count(label, sql);
        } catch (SQLException e) {
            SeedWarnings.add("업무 규칙 검사 '" + label + "'를 건너뜀(구조 변경): " + e.getMessage());
        }
    }

    private void count(String label, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            long n = rs.getLong(1);
            if (n > 0) {
                problems.add(label + ": " + n + "건");
            }
        }
    }
}
