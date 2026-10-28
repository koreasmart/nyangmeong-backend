package com.nyangmeong.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.auth.dto.AuthMeResponse;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.AuthMember;

import lombok.RequiredArgsConstructor;

/**
 * [인증 주체 조회 서비스]
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int VISIBLE_LOCAL_PART_LONG = 3;
    private static final int VISIBLE_LOCAL_PART_SHORT = 1;
    private static final int LONG_LOCAL_PART_MIN_LENGTH = 4;

    private final AuthMemberMapper authMemberMapper;
    private final FieldCryptoService fieldCryptoService;

    /**
     * 토큰 검증용 회원 조회
     */
    @Transactional(readOnly = true)
    public AuthMember getForAuth(Long mbrSn) {
        AuthMember member = authMemberMapper.selectForAuth(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /**
     * 내 정보 조회 (헤더 프로필/드롭다운)
     */
    @Transactional(readOnly = true)
    public AuthMeResponse getMe(Long mbrSn) {
        AuthMember member = authMemberMapper.selectForMe(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        String email = fieldCryptoService.decrypt(member.getAcntEmlEnc());
        return AuthMeResponse.of(member, maskEmail(email));
    }

    /**
     * 이메일 마스킹 (jin****@gmail.com)
     */
    static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return email;
        }
        String local = email.substring(0, at);
        int visible = local.length() >= LONG_LOCAL_PART_MIN_LENGTH ? VISIBLE_LOCAL_PART_LONG
                : VISIBLE_LOCAL_PART_SHORT;
        visible = Math.min(visible, local.length());
        return local.substring(0, visible) + "****" + email.substring(at);
    }
}
