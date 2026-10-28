package com.nyangmeong.auth.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.auth.domain.LoginAttempt;
import com.nyangmeong.auth.dto.CaptchaCreateRequest;
import com.nyangmeong.auth.dto.CaptchaResponse;
import com.nyangmeong.auth.dto.LoginAttemptLimitResponse;
import com.nyangmeong.auth.dto.LoginRequest;
import com.nyangmeong.auth.dto.LoginResponse;
import com.nyangmeong.auth.dto.LoginResult;
import com.nyangmeong.auth.mapper.LoginAttemptMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.captcha.CaptchaGenerator;
import com.nyangmeong.global.security.captcha.CaptchaGenerator.Captcha;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.service.MemberService;

import lombok.RequiredArgsConstructor;

/**
 * [이메일 로그인 서비스]
 * - 아이디(HMAC) 단위로 인증 실패를 누적하고, 제한 횟수에 닿으면 보안코드를 요구한다
 * - 없는 아이디·비밀번호 없는 SNS 가입 계정도 비밀번호 불일치와 똑같이 처리한다 (존재 여부 비공개)
 * - 실패는 예외가 아니라 LoginResult 로 돌려준다 : 실패 기록이 롤백되지 않아야 한다
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    /** 없는 아이디에도 같은 비용으로 비교하기 위한 형식만 맞는 BCrypt 해시 (어떤 비밀번호와도 맞지 않는다) */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa0Yx9Yqz9D1m5k0o8k0aG0nQK7vXy2u";

    private final LoginAttemptMapper loginAttemptMapper;
    private final MemberService memberService;
    private final SystemSettingService systemSettingService;
    private final FieldCryptoService fieldCryptoService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaGenerator captchaGenerator;

    /**
     * 아이디 기준 실패 횟수·보안코드 요구 상태 조회
     */
    @Transactional(readOnly = true)
    public LoginAttemptLimitResponse getAttemptLimit(String acntEml) {
        return toLimit(loginAttemptMapper.selectByIdHmac(fieldCryptoService.emailHmac(acntEml)));
    }

    /**
     * 보안코드 발급·새로고침 : 이전 코드는 무효, 실패 횟수와 요구 상태는 그대로
     */
    @Transactional
    public CaptchaResponse issueCaptcha(CaptchaCreateRequest req) {
        String idHmac = fieldCryptoService.emailHmac(req.acntEml());
        if (!isCaptchaRequired(loginAttemptMapper.selectByIdHmac(idHmac))) {
            throw new CustomException(ErrorCode.CAPTCHA_NOT_REQUIRED);
        }
        Captcha captcha = captchaGenerator.generate();
        loginAttemptMapper.updateCaptchaHash(idHmac, captchaHash(idHmac, captcha.code()));
        return new CaptchaResponse(captcha.imageDataUri());
    }

    /**
     * 이메일 로그인 : 보안코드 → 비밀번호 → 회원 상태 순서로 검사
     */
    @Transactional
    public LoginResult login(LoginRequest req) {
        String idHmac = fieldCryptoService.emailHmac(req.acntEml());
        LoginAttempt attempt = loginAttemptMapper.selectByIdHmac(idHmac);
        boolean captchaRequired = isCaptchaRequired(attempt);

        if (captchaRequired) {
            if (req.scrtyCd() == null || req.scrtyCd().isBlank()) {
                return fail(ErrorCode.LOGIN_CAPTCHA_REQUIRED, idHmac);
            }
            if (!matchesCaptcha(attempt, idHmac, req.scrtyCd())) {
                /** 틀린 코드도 한 번 쓴 것 : 새 코드를 받아야 한다. 실패 횟수는 늘리지 않는다 */
                loginAttemptMapper.clearCaptchaHash(idHmac);
                return fail(ErrorCode.CAPTCHA_MISMATCH, idHmac);
            }
        }

        Member member = memberService.findForLogin(idHmac);
        if (!matchesPassword(member, req.pswd())) {
            return failAuthentication(idHmac, captchaRequired);
        }
        if (MemberStatus.WITHDRAWN.is(member.getMbrSttsCd()) || MemberStatus.SUSPENDED.is(member.getMbrSttsCd())) {
            /** 거절돼도 쓴 보안코드는 다시 쓸 수 없다 */
            if (captchaRequired) {
                loginAttemptMapper.clearCaptchaHash(idHmac);
            }
            return fail(MemberStatus.WITHDRAWN.is(member.getMbrSttsCd()) ? ErrorCode.MEMBER_WITHDRAWN
                    : ErrorCode.MEMBER_INACTIVE, idHmac);
        }

        /** 최종 로그인 갱신 전 값으로 판단한다 */
        boolean ncnmReguide = "N".equals(member.getNcnmStngCmptnYn()) && member.getLastLgnDt() == null;
        memberService.recordLogin(member.getMbrSn());
        loginAttemptMapper.delete(idHmac);
        return LoginResult.success(member.getMbrSn(), new LoginResponse(ncnmReguide, member.getMbrNcnm()));
    }

    /** 비밀번호 불일치 : 횟수 +1, 이번에 처음 제한에 닿았으면 보안코드 안내 문구 */
    private LoginResult failAuthentication(String idHmac, boolean captchaWasRequired) {
        int limit = systemSettingService.get().getLgnFailLmtNmtm().intValue();
        loginAttemptMapper.upsertFailure(idHmac, limit);
        boolean justReached = !captchaWasRequired && isCaptchaRequired(loginAttemptMapper.selectByIdHmac(idHmac));
        return fail(justReached ? ErrorCode.LOGIN_CAPTCHA_REQUIRED : ErrorCode.LOGIN_FAILED, idHmac);
    }

    private LoginResult fail(ErrorCode error, String idHmac) {
        return LoginResult.fail(error, toLimit(loginAttemptMapper.selectByIdHmac(idHmac)));
    }

    /** 회원이 없거나 비밀번호가 없어도 BCrypt 비교를 한 번 한다 */
    private boolean matchesPassword(Member member, String rawPassword) {
        String hash = member != null ? member.getPswdHash() : null;
        if (hash == null) {
            passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
            return false;
        }
        return passwordEncoder.matches(rawPassword, hash);
    }

    private boolean matchesCaptcha(LoginAttempt attempt, String idHmac, String input) {
        String hash = attempt.getScrtyCdHash();
        return hash != null && hash.equals(captchaHash(idHmac, input.toUpperCase(Locale.ROOT)));
    }

    /** 코드 원문은 저장하지 않는다. 아이디가 섞여 다른 아이디의 코드로는 통과하지 못한다 */
    private String captchaHash(String idHmac, String code) {
        return fieldCryptoService.hmac("captcha:" + idHmac + ":" + code);
    }

    private boolean isCaptchaRequired(LoginAttempt attempt) {
        return attempt != null && "Y".equals(attempt.getScrtyCdRqtYn());
    }

    private LoginAttemptLimitResponse toLimit(LoginAttempt attempt) {
        if (attempt == null) {
            return new LoginAttemptLimitResponse(false, 0);
        }
        return new LoginAttemptLimitResponse(isCaptchaRequired(attempt),
                attempt.getCntnuCertFailNmtm() == null ? 0 : attempt.getCntnuCertFailNmtm());
    }
}
