package com.nyangmeong.member.dto;

/**
 * [닉네임 중복확인 응답]
 * - 현재 본인 닉네임이면 true
 */
public record MemberNicknameAvailabilityResponse(boolean available) {
}
