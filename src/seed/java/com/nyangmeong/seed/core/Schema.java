package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [실제 DB 구조]
 * - information_schema에서 컬럼·PK·유니크·FK를 읽는다. migration으로 바뀐 구조도 그대로 반영된다
 */
public final class Schema {

    private static final Pattern CODE_GROUP = Pattern.compile("\\(([A-Z]{3}G\\d{2})\\)");

    /** 테이블 → 컬럼 이름 → 컬럼 */
    private final Map<String, Map<String, Column>> tables;
    private final Map<String, List<String>> primaryKeys;
    /** 테이블 → 유니크 인덱스(컬럼 목록)들. PK 제외 */
    private final Map<String, List<List<String>>> uniqueKeys;
    private final List<ForeignKey> foreignKeys;

    private Schema(Map<String, Map<String, Column>> tables, Map<String, List<String>> primaryKeys,
            Map<String, List<List<String>>> uniqueKeys, List<ForeignKey> foreignKeys) {
        this.tables = tables;
        this.primaryKeys = primaryKeys;
        this.uniqueKeys = uniqueKeys;
        this.foreignKeys = foreignKeys;
    }

    /**
     * @param nullable    NULL 허용
     * @param hasDefault  기본값 있음(넣지 않아도 된다)
     * @param generated   생성 컬럼(넣으면 안 된다)
     * @param codeGroup   주석의 공통코드 그룹, 없으면 null
     * @param dataType    소문자 타입(varchar, decimal, datetime …)
     * @param maxLength   문자 길이, 문자가 아니면 null
     * @param precision   숫자 전체 자릿수, 숫자가 아니면 null
     * @param scale       숫자 소수 자릿수
     * @param comment     컬럼 주석(한글 이름)
     */
    public record Column(String name, boolean nullable, boolean hasDefault, boolean generated, boolean autoIncrement,
            String codeGroup, String dataType, Long maxLength, Integer precision, Integer scale, String comment) {

        public boolean requiresValue() {
            return !nullable && !hasDefault && !generated && !autoIncrement;
        }
    }

    public record ForeignKey(String table, String column, String refTable, String refColumn) {
    }

    public static Schema load(Connection conn) throws SQLException {
        Map<String, Map<String, Column>> tables = new LinkedHashMap<>();
        String columnSql = """
                SELECT UPPER(TABLE_NAME), COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT,
                       LOWER(DATA_TYPE), CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE
                  FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                 ORDER BY TABLE_NAME, ORDINAL_POSITION""";
        try (PreparedStatement ps = conn.prepareStatement(columnSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String extra = rs.getString(5).toUpperCase();
                String comment = rs.getString(6);
                Matcher group = CODE_GROUP.matcher(comment);
                Column column = new Column(rs.getString(2), "YES".equals(rs.getString(3)),
                        rs.getString(4) != null || extra.contains("DEFAULT_GENERATED"),
                        extra.contains("VIRTUAL GENERATED") || extra.contains("STORED GENERATED"),
                        extra.contains("AUTO_INCREMENT"), group.find() ? group.group(1) : null, rs.getString(7),
                        (Long) rs.getObject(8, Long.class), (Integer) rs.getObject(9, Integer.class),
                        (Integer) rs.getObject(10, Integer.class), comment);
                tables.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(column.name(), column);
            }
        }
        Map<String, List<String>> primaryKeys = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> indexes = new LinkedHashMap<>();
        String indexSql = """
                SELECT UPPER(TABLE_NAME), INDEX_NAME, COLUMN_NAME
                  FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND NON_UNIQUE = 0
                 ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX""";
        try (PreparedStatement ps = conn.prepareStatement(indexSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1);
                if ("PRIMARY".equals(rs.getString(2))) {
                    primaryKeys.computeIfAbsent(table, k -> new ArrayList<>()).add(rs.getString(3));
                } else {
                    indexes.computeIfAbsent(table, k -> new LinkedHashMap<>())
                            .computeIfAbsent(rs.getString(2), k -> new ArrayList<>()).add(rs.getString(3));
                }
            }
        }
        Map<String, List<List<String>>> uniqueKeys = new LinkedHashMap<>();
        indexes.forEach((table, byName) -> uniqueKeys.put(table, new ArrayList<>(byName.values())));
        List<ForeignKey> foreignKeys = new ArrayList<>();
        String fkSql = """
                SELECT UPPER(TABLE_NAME), COLUMN_NAME, UPPER(REFERENCED_TABLE_NAME), REFERENCED_COLUMN_NAME
                  FROM information_schema.KEY_COLUMN_USAGE
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND REFERENCED_TABLE_NAME IS NOT NULL""";
        try (PreparedStatement ps = conn.prepareStatement(fkSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                foreignKeys.add(new ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return new Schema(tables, primaryKeys, uniqueKeys, foreignKeys);
    }

    public boolean hasTable(String table) {
        return tables.containsKey(table);
    }

    public Set<String> tableNames() {
        return tables.keySet();
    }

    public Map<String, Column> columns(String table) {
        Map<String, Column> columns = tables.get(table);
        if (columns == null) {
            throw new IllegalStateException("DB에 " + table + " 테이블이 없습니다. 기준 SQL과 db/migration을 확인해 주세요");
        }
        return columns;
    }

    /** 단일 컬럼 PK면 그 이름, 아니면 null */
    public String singlePrimaryKey(String table) {
        List<String> pk = primaryKeys.get(table);
        return pk != null && pk.size() == 1 ? pk.get(0) : null;
    }

    public List<List<String>> uniqueKeys(String table) {
        return uniqueKeys.getOrDefault(table, List.of());
    }

    /** 한 컬럼만으로 된 유니크 인덱스가 걸린 컬럼인지 */
    public boolean isUniqueAlone(String table, String column) {
        return uniqueKeys(table).stream().anyMatch(k -> k.size() == 1 && k.get(0).equals(column));
    }

    public List<ForeignKey> foreignKeys() {
        return foreignKeys;
    }

    public ForeignKey foreignKey(String table, String column) {
        return foreignKeys.stream().filter(fk -> fk.table().equals(table) && fk.column().equals(column)).findFirst()
                .orElse(null);
    }
}
