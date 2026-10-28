package com.nyangmeong.global.log;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * [민감정보 마스킹]
 * - 로그/오류 응답의 이메일·휴대전화번호 가림
 */
@Component
public class SensitiveDataMasker {

    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    /** 010-XXXX-XXXX / 01XXXXXXXXX / +82 10-XXXX-XXXX */
    private static final Pattern PHONE = Pattern.compile("(?:\\+82[- ]?|0)(1[016789])[- ]?(\\d{3,4})[- ]?(\\d{4})");

    public String maskText(String text) {
        if (text == null) {
            return null;
        }
        String masked = EMAIL.matcher(text).replaceAll("$1***@$2");
        return PHONE.matcher(masked).replaceAll("0$1-****-$3");
    }

    /**
     * URI 디코딩 후 마스킹
     */
    public String maskUri(String uri) {
        if (uri == null) {
            return null;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = uri;
        }
        return maskText(decoded);
    }
}
