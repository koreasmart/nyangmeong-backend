package com.nyangmeong.seed.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [시드 행 하나]
 * - 컬럼 이름 → 값. 넣지 않은 컬럼은 DB 기본값을 쓴다(NULL로 넣지 않는다)
 */
public final class Row {

    private final String table;
    private final long sn;
    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * @param pkColumn PK 컬럼 이름(보통 테이블명_SN)
     */
    Row(String table, String pkColumn, long sn) {
        this.table = table;
        this.sn = sn;
        values.put(pkColumn, sn);
    }

    public long sn() {
        return sn;
    }

    public Row set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    /**
     * Y/N 컬럼
     */
    public Row yn(String column, boolean value) {
        return set(column, value ? "Y" : "N");
    }

    public Object get(String column) {
        return values.get(column);
    }

    public String string(String column) {
        return (String) values.get(column);
    }

    void setIfAbsent(String column, Object value) {
        values.putIfAbsent(column, value);
    }

    Map<String, Object> values() {
        return values;
    }

    @Override
    public String toString() {
        return table + "#" + sn;
    }
}
