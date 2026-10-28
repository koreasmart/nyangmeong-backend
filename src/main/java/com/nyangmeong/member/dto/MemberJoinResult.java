package com.nyangmeong.member.dto;

/**
 * [회원가입 처리 결과]
 * - 컨트롤러가 닉네임 설정 쿠키를 발급하려면 응답에 없는 회원번호가 필요하다
 */
public record MemberJoinResult(Long mbrSn, MemberResponse response) {
}
