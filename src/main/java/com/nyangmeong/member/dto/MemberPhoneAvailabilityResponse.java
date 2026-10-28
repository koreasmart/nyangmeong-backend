package com.nyangmeong.member.dto;

/**
 * [휴대전화번호 중복확인 응답]
 * - normalizedMblTelno : 공백·하이픈 제거, +82 를 국내 표기로 통일한 값
 */
public record MemberPhoneAvailabilityResponse(String normalizedMblTelno, boolean available) {
}
