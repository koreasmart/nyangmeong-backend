package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.SeedException;

/**
 * [공통코드]
 * - DB의 COM_CD를 읽어 그룹·이름으로 코드를 찾는다. 없는 이름이면 넣기 전에 멈춘다
 */
public final class Codes {

    /** 그룹 코드 → 코드 이름 → 코드 */
    private final Map<String, Map<String, String>> groups;

    private Codes(Map<String, Map<String, String>> groups) {
        this.groups = groups;
    }

    static Codes load(Connection conn) throws SQLException {
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();
        String sql = """
                SELECT g.COM_CD, c.COM_CD, c.COM_CD_NM
                  FROM COM_CD c
                  JOIN COM_CD g ON g.COM_CD_SN = c.COM_CD_PRNT_SN
                 WHERE c.USE_YN = 'Y'
                 ORDER BY g.SORT_SEQ, c.SORT_SEQ""";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(rs.getString(3), rs.getString(2));
            }
        }
        if (groups.isEmpty()) {
            throw new SeedException("COM_CD가 비어 있습니다. db/의 공통코드 SQL을 먼저 실행해 주세요");
        }
        return new Codes(groups);
    }

    /**
     * 이름으로 코드 찾기
     * - 이름이 바뀌었거나 지워졌으면 같은 그룹의 첫 코드로 대신하고 경고를 남긴다
     */
    public String of(String group, String name) {
        Map<String, String> codes = group(group);
        String code = codes.get(name);
        if (code == null) {
            code = codes.values().iterator().next();
            SeedWarnings.add("공통코드 " + group + "에 '" + name + "'이 없어 " + code + "로 대신함");
        }
        return code;
    }

    public boolean hasGroup(String group) {
        Map<String, String> codes = groups.get(group);
        return codes != null && !codes.isEmpty();
    }

    /** 그룹이 없는 코드 컬럼에 넣을 아무 코드(자동 채움용) */
    public String anyCode() {
        return groups.values().iterator().next().values().iterator().next();
    }

    public List<String> all(String group) {
        return new ArrayList<>(group(group).values());
    }

    /**
     * 코드 → 이름
     */
    public String name(String group, String code) {
        return group(group).entrySet().stream()
                .filter(e -> e.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new SeedException("공통코드 " + group + "에 " + code + "가 없습니다"));
    }

    private Map<String, String> group(String group) {
        Map<String, String> codes = groups.get(group);
        if (codes == null || codes.isEmpty()) {
            throw new SeedException("공통코드 그룹 " + group + "이 없습니다");
        }
        return codes;
    }
}
