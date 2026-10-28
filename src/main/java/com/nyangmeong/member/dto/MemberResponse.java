package com.nyangmeong.member.dto;

import java.util.List;

/**
 * [회원 응답]
 * - POST /api/members, PATCH /api/members/me/nickname
 * - 이메일·휴대전화번호는 복호화한 값
 */
public record MemberResponse(String acntEml, String mbrNcnm, String mblTelno, Long atchFileSn, String joinPathCd,
        boolean ncnmStngCmptnYn, List<MemberSnsAccountSummary> snsAcntLinks) {
}
