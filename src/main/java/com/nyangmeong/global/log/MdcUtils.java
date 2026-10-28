package com.nyangmeong.global.log;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * [MDC 유틸]
 * - 요청 추적용 traceId/clientIp 키
 */
public final class MdcUtils {

    public static final String TRACE_ID = "traceId";
    public static final String CLIENT_IP = "clientIp";

    private MdcUtils() {
    }

    public static void put(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * MDC 정리 (스레드 풀 재사용 대비)
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * traceId 생성 (UUID 앞 8자리)
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
