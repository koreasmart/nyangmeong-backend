package com.nyangmeong.auth.dto;

/**
 * [보안코드 응답]
 * - captchaImg : PNG 를 Base64 로 담은 data URI. 코드 원문은 주지 않는다
 */
public record CaptchaResponse(String captchaImg) {
}
