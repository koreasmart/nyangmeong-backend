package com.nyangmeong.seed.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [자동 엔진]
 * - 테이블 구조가 바뀌어도 생성기를 고치지 않고 돌게 한다. 스스로 메운 것은 SeedWarnings에 남긴다
 * - reconcile : Seeder가 만든 행을 실제 DB 구조에 맞춘다(없는 컬럼·테이블 빼기, 값 없는 NOT NULL 채우기)
 * - generateUnknownTables : 어느 Seeder도 모르는 새 테이블에 행을 만든다(FK 순서, 유니크 겹치지 않게)
 * - 값은 컬럼 타입·주석(한글 이름)·공통코드 그룹·FK로 정한다. 업무 규칙은 모르므로 화면이 어색하면 Seeder로 다듬는다
 */
public final class AutoFill {

    private static final int AUTO_ROWS = 50;
    private static final int UNIQUE_RETRIES = 10;

    private final Seed seed;
    private final Schema schema;
    private final RowStore store;

    public AutoFill(Seed seed, Schema schema) {
        this.seed = seed;
        this.schema = schema;
        this.store = seed.store();
    }

    /**
     * Seeder가 만든 행을 DB 구조에 맞추기
     */
    public void reconcile() {
        for (String table : new ArrayList<>(store.tableNames())) {
            if (!schema.hasTable(table)) {
                SeedWarnings.add(table + " 테이블이 DB에 없어 Seeder가 만든 " + store.rows(table).size() + "행을 넣지 않음");
                store.removeTable(table);
                continue;
            }
            Map<String, Schema.Column> columns = schema.columns(table);
            for (Row row : store.rows(table)) {
                for (String name : new ArrayList<>(row.values().keySet())) {
                    Schema.Column column = columns.get(name);
                    if (column == null || column.generated()) {
                        row.values().remove(name);
                        SeedWarnings.add(table + "." + name + (column == null ? " 컬럼이 DB에 없어 빼고 넣음" : " 생성 컬럼이라 빼고 넣음"));
                    }
                }
                for (Schema.Column column : columns.values()) {
                    if (column.requiresValue() && !row.values().containsKey(column.name())) {
                        row.set(column.name(), value(table, column, row.sn()));
                        SeedWarnings.add(table + "." + column.name() + " 값이 없어 자동으로 채움");
                    }
                }
            }
        }
    }

    /**
     * 모르는 테이블에 행 만들기
     * @param known Seeder가 맡거나 일부러 비워 두는 테이블
     * @return 자동으로 만든 테이블(넣는 순서)
     */
    public List<String> generateUnknownTables(Set<String> known) {
        List<String> unknown = new ArrayList<>();
        for (String table : schema.tableNames()) {
            if (!known.contains(table) && !store.tableNames().contains(table)) {
                unknown.add(table);
            }
        }
        List<String> made = new ArrayList<>();
        for (String table : fkOrder(unknown)) {
            String pk = schema.singlePrimaryKey(table);
            if (pk == null) {
                SeedWarnings.add("새 테이블 " + table + ": 한 컬럼 PK가 아니라 자동 생성을 건너뜀(Seeder 필요)");
                continue;
            }
            String missingParent = missingRequiredParent(table);
            if (missingParent != null) {
                SeedWarnings.add("새 테이블 " + table + ": 필수 FK가 가리키는 " + missingParent + "에 행이 없어 건너뜀");
                continue;
            }
            int target = seed.scaled(AUTO_ROWS);
            Map<List<String>, Set<List<Object>>> seen = new LinkedHashMap<>();
            for (int i = 0; i < target; i++) {
                addUniqueRow(table, pk, seen);
            }
            int count = store.rows(table).size();
            if (count > 0) {
                made.add(table);
                SeedWarnings.add("새 테이블 " + table + ": Seeder가 없어 자동으로 " + count + "행 생성");
            }
        }
        return made;
    }

    private void addUniqueRow(String table, String pk, Map<List<String>, Set<List<Object>>> seen) {
        for (int attempt = 0; attempt < UNIQUE_RETRIES; attempt++) {
            long sn = store.rows(table).size() + 1L;
            Map<String, Object> values = new LinkedHashMap<>();
            for (Schema.Column column : schema.columns(table).values()) {
                if (column.name().equals(pk) || column.generated()) {
                    continue;
                }
                boolean fk = schema.foreignKey(table, column.name()) != null;
                if (column.requiresValue() || column.nullable() && seed.chance(fk ? 0.5 : 0.7)) {
                    values.put(column.name(), value(table, column, sn));
                }
            }
            if (isNew(table, values, seen)) {
                store.add(table, pk, r -> values.forEach(r::set));
                return;
            }
        }
    }

    /** 유니크 인덱스마다 값 조합이 처음이면 기록하고 true. NULL이 낀 조합은 겹쳐도 된다 */
    private boolean isNew(String table, Map<String, Object> values, Map<List<String>, Set<List<Object>>> seen) {
        List<List<Object>> keys = new ArrayList<>();
        for (List<String> index : schema.uniqueKeys(table)) {
            List<Object> key = index.stream().map(values::get).toList();
            if (key.contains(null)) {
                keys.add(null);
                continue;
            }
            if (seen.getOrDefault(index, Set.of()).contains(key)) {
                return false;
            }
            keys.add(key);
        }
        List<List<String>> indexes = schema.uniqueKeys(table);
        for (int i = 0; i < indexes.size(); i++) {
            if (keys.get(i) != null) {
                seen.computeIfAbsent(indexes.get(i), k -> new HashSet<>()).add(keys.get(i));
            }
        }
        return true;
    }

    private String missingRequiredParent(String table) {
        for (Schema.Column column : schema.columns(table).values()) {
            Schema.ForeignKey fk = schema.foreignKey(table, column.name());
            if (fk != null && column.requiresValue() && !fk.refTable().equals("COM_CD") && !fk.refTable().equals(table)
                    && store.rows(fk.refTable()).isEmpty()) {
                return fk.refTable();
            }
        }
        return null;
    }

    /** 부모(FK 대상)가 먼저 오게. 순환이면 남은 순서대로 */
    private List<String> fkOrder(List<String> tables) {
        Set<String> ordered = new LinkedHashSet<>();
        Set<String> pending = new LinkedHashSet<>(tables);
        boolean progressed = true;
        while (!pending.isEmpty() && progressed) {
            progressed = false;
            for (String table : new ArrayList<>(pending)) {
                boolean ready = schema.foreignKeys().stream()
                        .filter(fk -> fk.table().equals(table) && !fk.refTable().equals(table))
                        .noneMatch(fk -> pending.contains(fk.refTable()));
                if (ready) {
                    ordered.add(table);
                    pending.remove(table);
                    progressed = true;
                }
            }
        }
        ordered.addAll(pending);
        return new ArrayList<>(ordered);
    }

    // ---- 값 정하기

    /**
     * 컬럼 하나의 값
     * - FK → 공통코드 그룹 → 이름 규칙(_YN·금액·수·경로·해시·암호문 …) → 타입 순서로 정한다
     */
    Object value(String table, Schema.Column column, long sn) {
        String name = column.name().toUpperCase();
        String comment = column.comment() == null ? "" : column.comment();
        boolean unique = schema.isUniqueAlone(table, column.name());
        Schema.ForeignKey fk = schema.foreignKey(table, column.name());
        if (fk != null && fk.refTable().equals("COM_CD") || column.codeGroup() != null) {
            return code(column);
        }
        if (fk != null) {
            List<Row> parents = store.rows(fk.refTable());
            if (parents.isEmpty()) {
                return fk.refTable().equals(table) ? sn : null;
            }
            return seed.pick(parents).sn();
        }
        String type = column.dataType();
        return switch (type) {
        case "char", "varchar", "text", "tinytext", "mediumtext", "longtext" -> text(column, name, comment, sn, unique);
        case "tinyint", "smallint", "mediumint", "int", "bigint", "decimal", "float", "double" ->
                number(column, name, comment, sn, unique);
        case "date" -> seed.today().minusDays(seed.between(0, 60));
        case "datetime", "timestamp" -> seed.daysAgo(0, 60);
        case "time" -> "12:00:00";
        case "year" -> seed.today().getYear();
        case "json" -> "{}";
        case "bit", "boolean" -> 0;
        default -> {
            SeedWarnings.add(table + "." + column.name() + " 타입 " + type + "는 자동으로 채우지 못함");
            yield null;
        }
        };
    }

    private String code(Schema.Column column) {
        Codes codes = seed.codes();
        if (column.codeGroup() != null && codes.hasGroup(column.codeGroup())) {
            return seed.pick(codes.all(column.codeGroup()));
        }
        return codes.anyCode();
    }

    private String text(Schema.Column column, String name, String comment, long sn, boolean unique) {
        long max = column.maxLength() == null ? 200 : column.maxLength();
        String value;
        if (name.endsWith("_YN") || max == 1) {
            value = name.startsWith("USE") || name.contains("_USE_") ? "Y" : "N";
        } else if (name.endsWith("_ENC")) {
            value = seed.crypto().encrypt("시드 " + label(comment) + " " + sn);
        } else if (name.endsWith("_HMAC") || name.endsWith("_HASH")) {
            value = seed.hex64();
        } else if (name.contains("EML")) {
            value = "seed" + sn + "@example.com";
        } else if (name.contains("TELNO")) {
            value = String.format("010%08d", sn);
        } else if (name.endsWith("_PATH") || name.endsWith("_URL")) {
            value = "/seed/" + sn;
        } else if (name.endsWith("_ID") || name.endsWith("_KEY")) {
            value = "SEED-" + sn;
        } else if (name.endsWith("_CN") || comment.contains("내용")) {
            value = Fake.paragraph(seed, 1, 2);
        } else if ("char".equals(column.dataType())) {
            value = String.format("%0" + max + "d", sn % (long) Math.pow(10, Math.min(max, 18)));
        } else {
            value = label(comment).isEmpty() ? "시드" : "시드 " + label(comment);
        }
        if (unique && !name.endsWith("_HMAC") && !name.endsWith("_HASH") && !name.endsWith("_ENC")
                && !value.endsWith(String.valueOf(sn))) {
            value = value + " " + sn;
        }
        return value.length() > max ? value.substring(value.length() - (int) max) : value;
    }

    private Object number(Schema.Column column, String name, String comment, long sn, boolean unique) {
        long value;
        if (unique) {
            value = sn;
        } else if (name.endsWith("_AMT") || comment.contains("금액")) {
            value = seed.between(1, 100) * 1000L;
        } else if (name.endsWith("_SEQ") || comment.contains("순서")) {
            value = sn * 10;
        } else if (name.endsWith("_CNT") || name.endsWith("_NMTM") || comment.endsWith("수")) {
            value = seed.between(0, 10);
        } else if (name.endsWith("_LAT") || comment.contains("위도")) {
            return scaled(column, seed.between(33.2, 38.0));
        } else if (name.endsWith("_LOT") || comment.contains("경도")) {
            return scaled(column, seed.between(126.0, 129.5));
        } else {
            value = seed.between(1, 10);
        }
        if (column.precision() != null) {
            int digits = column.precision() - (column.scale() == null ? 0 : column.scale());
            long max = digits >= 18 ? Long.MAX_VALUE : (long) Math.pow(10, digits) - 1;
            value = Math.min(value, max);
        }
        return "decimal".equals(column.dataType()) ? scaled(column, value) : value;
    }

    private BigDecimal scaled(Schema.Column column, double value) {
        return BigDecimal.valueOf(value).setScale(column.scale() == null ? 0 : column.scale(), RoundingMode.HALF_UP);
    }

    /** 주석에서 공통코드 표기를 뺀 한글 이름 */
    private static String label(String comment) {
        return comment.replaceAll("\\([A-Z]{3}G\\d{2}\\)", "").trim();
    }
}
