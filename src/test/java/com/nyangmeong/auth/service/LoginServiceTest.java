package com.nyangmeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nyangmeong.auth.domain.LoginAttempt;
import com.nyangmeong.auth.dto.CaptchaCreateRequest;
import com.nyangmeong.auth.dto.LoginRequest;
import com.nyangmeong.auth.dto.LoginResult;
import com.nyangmeong.auth.mapper.LoginAttemptMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.captcha.CaptchaGenerator;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.service.MemberService;

/**
 * [검사 - 이메일 로그인 서비스]
 * - 실패 누적·보안코드 요구·탈퇴/정지·첫 로그인 재안내
 */
class LoginServiceTest {

    private static final String EMAIL = "choco@example.com";
    private static final String PASSWORD = "pass1234";

    private final LoginAttemptMapper loginAttemptMapper = mock(LoginAttemptMapper.class);
    private final MemberService memberService = mock(MemberService.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final FieldCryptoService fieldCryptoService = cryptoService();
    private final CaptchaGenerator captchaGenerator = new CaptchaGenerator();
    private final LoginService service = new LoginService(loginAttemptMapper, memberService, systemSettingService,
            fieldCryptoService, passwordEncoder, captchaGenerator);

    private final String idHmac = fieldCryptoService.emailHmac(EMAIL);

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService crypto = new FieldCryptoService(properties);
        crypto.initializeKeys();
        return crypto;
    }

    private Member member(String statusCode, String ncnmStngCmptnYn, LocalDateTime lastLgnDt) {
        Member member = new Member();
        member.setMbrSn(7L);
        member.setMbrSttsCd(statusCode);
        member.setPswdHash(passwordEncoder.encode(PASSWORD));
        member.setMbrNcnm("초코맘");
        member.setNcnmStngCmptnYn(ncnmStngCmptnYn);
        member.setLastLgnDt(lastLgnDt);
        return member;
    }

    private LoginAttempt attempt(int failCount, boolean captchaRequired, String captchaHash) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setCntnuCertFailNmtm(failCount);
        attempt.setScrtyCdRqtYn(captchaRequired ? "Y" : "N");
        attempt.setScrtyCdHash(captchaHash);
        return attempt;
    }

    private void givenLimit(int limit) {
        SystemSetting setting = new SystemSetting();
        setting.setLgnFailLmtNmtm((long) limit);
        when(systemSettingService.get()).thenReturn(setting);
    }

    @Test
    void 기록_없는_아이디의_상태는_false_0이다() {
        assertThat(service.getAttemptLimit(EMAIL).scrtyCdRqtYn()).isFalse();
        assertThat(service.getAttemptLimit(EMAIL).cntnuCertFailNmtm()).isZero();
    }

    @Test
    void 기록이_있으면_횟수와_보안코드_요구를_돌려준다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, null));

        assertThat(service.getAttemptLimit(EMAIL).scrtyCdRqtYn()).isTrue();
        assertThat(service.getAttemptLimit(EMAIL).cntnuCertFailNmtm()).isEqualTo(5);
    }

    @Test
    void 이메일_대소문자와_공백이_달라도_같은_아이디로_본다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(2, false, null));

        assertThat(service.getAttemptLimit("  Choco@Example.com ").cntnuCertFailNmtm()).isEqualTo(2);
    }

    @Test
    void 비밀번호가_틀리면_LOGIN_FAILED이고_횟수를_올린다() {
        givenLimit(5);
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", null));
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(null, attempt(1, false, null));

        LoginResult result = service.login(new LoginRequest(EMAIL, "wrong1234", null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_FAILED);
        assertThat(result.limits().cntnuCertFailNmtm()).isEqualTo(1);
        verify(loginAttemptMapper).upsertFailure(idHmac, 5);
        verify(memberService, never()).recordLogin(7L);
    }

    @Test
    void 없는_아이디도_LOGIN_FAILED이고_횟수를_올린다() {
        givenLimit(5);
        when(memberService.findForLogin(idHmac)).thenReturn(null);

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_FAILED);
        verify(loginAttemptMapper).upsertFailure(idHmac, 5);
    }

    @Test
    void 비밀번호_없는_SNS_가입_계정도_LOGIN_FAILED이고_횟수를_올린다() {
        givenLimit(5);
        Member snsMember = member(MemberStatus.NORMAL.getCode(), "Y", null);
        snsMember.setPswdHash(null);
        when(memberService.findForLogin(idHmac)).thenReturn(snsMember);

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_FAILED);
        verify(loginAttemptMapper).upsertFailure(idHmac, 5);
    }

    @Test
    void 다섯_번째_실패는_보안코드_안내_LOGIN_CAPTCHA_REQUIRED다() {
        givenLimit(5);
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", null));
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(4, false, null),
                attempt(5, true, null), attempt(5, true, null));

        LoginResult result = service.login(new LoginRequest(EMAIL, "wrong1234", null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_CAPTCHA_REQUIRED);
        assertThat(result.limits().scrtyCdRqtYn()).isTrue();
        assertThat(result.limits().cntnuCertFailNmtm()).isEqualTo(5);
    }

    @Test
    void 보안코드를_요구하는데_코드가_없으면_횟수를_올리지_않고_거절한다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, null));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_CAPTCHA_REQUIRED);
        verify(loginAttemptMapper, never()).upsertFailure(anyString(), anyInt());
        verify(memberService, never()).findForLogin(anyString());
    }

    @Test
    void 틀린_보안코드는_CAPTCHA_MISMATCH이고_코드를_비우며_횟수는_그대로다() {
        String issued = fieldCryptoService.hmac("captcha:" + idHmac + ":AB12CD");
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, issued));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, "ZZ99ZZ"));

        assertThat(result.error()).isEqualTo(ErrorCode.CAPTCHA_MISMATCH);
        verify(loginAttemptMapper).clearCaptchaHash(idHmac);
        verify(loginAttemptMapper, never()).upsertFailure(anyString(), anyInt());
    }

    @Test
    void 발급한_코드가_없으면_어떤_코드도_틀린_것이다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, null));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, "AB12CD"));

        assertThat(result.error()).isEqualTo(ErrorCode.CAPTCHA_MISMATCH);
    }

    @Test
    void 보안코드가_맞고_비밀번호가_틀리면_LOGIN_FAILED이고_새_코드를_받게_한다() {
        givenLimit(5);
        String issued = fieldCryptoService.hmac("captcha:" + idHmac + ":AB12CD");
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, issued));
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", null));

        LoginResult result = service.login(new LoginRequest(EMAIL, "wrong1234", "ab12cd"));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_FAILED);
        verify(loginAttemptMapper).upsertFailure(idHmac, 5);
    }

    @Test
    void 보안코드가_맞고_비밀번호도_맞으면_로그인되고_기록을_지운다() {
        String issued = fieldCryptoService.hmac("captcha:" + idHmac + ":AB12CD");
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, issued));
        when(memberService.findForLogin(idHmac)).thenReturn(
                member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.of(2026, 10, 1, 9, 0)));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, "ab12cd"));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.mbrSn()).isEqualTo(7L);
        verify(memberService).recordLogin(7L);
        verify(loginAttemptMapper).delete(idHmac);
    }

    @Test
    void 비밀번호가_맞은_탈퇴_회원은_MEMBER_WITHDRAWN이고_기록을_지우지_않는다() {
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.WITHDRAWN.getCode(), "Y", null));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(result.error()).isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
        verify(memberService, never()).recordLogin(7L);
        verify(loginAttemptMapper, never()).delete(idHmac);
        verify(loginAttemptMapper, never()).upsertFailure(anyString(), anyInt());
    }

    @Test
    void 비밀번호가_맞은_정지_회원은_MEMBER_INACTIVE다() {
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.SUSPENDED.getCode(), "Y", null));

        LoginResult result = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(result.error()).isEqualTo(ErrorCode.MEMBER_INACTIVE);
        verify(memberService, never()).recordLogin(7L);
    }

    @Test
    void 비밀번호가_틀린_탈퇴_회원은_탈퇴_여부를_숨기고_LOGIN_FAILED다() {
        givenLimit(5);
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.WITHDRAWN.getCode(), "Y", null));

        LoginResult result = service.login(new LoginRequest(EMAIL, "wrong1234", null));

        assertThat(result.error()).isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void 닉네임_미완료인_첫_로그인만_재안내한다() {
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.NORMAL.getCode(), "N", null));

        LoginResult first = service.login(new LoginRequest(EMAIL, PASSWORD, null));

        assertThat(first.response().ncnmReguide()).isTrue();
        assertThat(first.response().mbrNcnm()).isEqualTo("초코맘");
    }

    @Test
    void 이전_로그인_기록이_있으면_닉네임_미완료여도_재안내하지_않는다() {
        when(memberService.findForLogin(idHmac)).thenReturn(
                member(MemberStatus.NORMAL.getCode(), "N", LocalDateTime.of(2026, 10, 1, 9, 0)));

        assertThat(service.login(new LoginRequest(EMAIL, PASSWORD, null)).response().ncnmReguide()).isFalse();
    }

    @Test
    void 닉네임을_이미_정했으면_첫_로그인도_재안내하지_않는다() {
        when(memberService.findForLogin(idHmac)).thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", null));

        assertThat(service.login(new LoginRequest(EMAIL, PASSWORD, null)).response().ncnmReguide()).isFalse();
    }

    @Test
    void 보안코드를_요구하지_않는_아이디의_발급은_CAPTCHA_NOT_REQUIRED다() {
        assertThatThrownBy(() -> service.issueCaptcha(new CaptchaCreateRequest(EMAIL)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_NOT_REQUIRED));
        verify(loginAttemptMapper, never()).updateCaptchaHash(anyString(), anyString());
    }

    @Test
    void 요구_상태_아이디의_발급은_이미지를_주고_해시만_저장한다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, null));

        String image = service.issueCaptcha(new CaptchaCreateRequest(EMAIL)).captchaImg();

        assertThat(image).startsWith("data:image/png;base64,");
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(loginAttemptMapper).updateCaptchaHash(eq(idHmac), hash.capture());
        assertThat(hash.getValue()).hasSize(64).matches("[0-9a-f]{64}");
        verify(loginAttemptMapper, never()).upsertFailure(anyString(), anyInt());
    }

    @Test
    void 새로고침하면_해시가_다시_저장된다() {
        when(loginAttemptMapper.selectByIdHmac(idHmac)).thenReturn(attempt(5, true, "old"));

        service.issueCaptcha(new CaptchaCreateRequest(EMAIL));
        service.issueCaptcha(new CaptchaCreateRequest(EMAIL));

        verify(loginAttemptMapper, times(2)).updateCaptchaHash(eq(idHmac), anyString());
    }
}
