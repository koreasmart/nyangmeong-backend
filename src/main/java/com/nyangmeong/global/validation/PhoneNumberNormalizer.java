package com.nyangmeong.global.validation;

import java.util.regex.Pattern;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [휴대전화번호 정규화]
 * - 공백·하이픈 제거, +82 표기를 국내 표기(01012345678)로 통일
 * - 암호화·HMAC·중복 비교는 이 결과로만 한다
 */
public final class PhoneNumberNormalizer {

    private static final Pattern DOMESTIC_MOBILE = Pattern.compile("^01[016789]\\d{7,8}$");
    private static final String COUNTRY_PREFIX = "+82";

    private PhoneNumberNormalizer() {
    }

    /**
     * 정규화 : 형식이 맞지 않으면 INVALID_PHONE_NUMBER
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new CustomException(ErrorCode.INVALID_PHONE_NUMBER);
        }
        String value = raw.replaceAll("[\\s-]", "");
        if (value.startsWith(COUNTRY_PREFIX)) {
            String rest = value.substring(COUNTRY_PREFIX.length());
            /** +82 010... 처럼 0을 남긴 표기도 같은 번호 */
            value = rest.startsWith("0") ? rest : "0" + rest;
        }
        if (!DOMESTIC_MOBILE.matcher(value).matches()) {
            throw new CustomException(ErrorCode.INVALID_PHONE_NUMBER);
        }
        return value;
    }
}
