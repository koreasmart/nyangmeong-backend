package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.member.domain.EmailCertPurpose;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.dto.EmailCertificationCreateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.EmailCertificationResult;
import com.nyangmeong.member.dto.EmailCertificationVerifyRequest;
import com.nyangmeong.member.mapper.EmailCertificationMapper;

/**
 * [검사 - 이메일 인증 서비스]
 * - 발송 제한(간격·횟수)·재전송 무효화·확인 실패 누적·가입 때 인증 검사
 */
class EmailCertificationServiceTest {

    private static final String EMAIL = "newbie@example.com";
    private static final String CODE = "123456";

    private final EmailCertificationMapper mapper = mock(EmailCertificationMapper.class);
    private final MemberService memberService = mock(MemberService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final FieldCryptoService crypto = cryptoService();
    private final EmailCertProperties properties = new EmailCertProperties();
    private final EmailCertificationService service = new EmailCertificationService(mapper, memberService, crypto,
            eventPublisher, properties);

    private final String emailHmac = crypto.hmac(EMAIL);

    private static FieldCryptoService cryptoService() {
        CryptoProperties cryptoProperties = new CryptoProperties();
        cryptoProperties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        cryptoProperties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(cryptoProperties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private EmailCertificationCreateRequest sendRequest() {
        return new EmailCertificationCreateRequest(EmailCertPurpose.SIGNUP.getCode(), " Newbie@Example.com ");
    }

    private EmailCertification cert(int failCount, LocalDateTime expiry, LocalDateTime completed) {
        EmailCertification cert = new EmailCertification();
        cert.setEmlCertSn(3L);
        cert.setEmlCertPrpsCd(EmailCertPurpose.SIGNUP.getCode());
        cert.setCertTrgtEmlEnc(crypto.encrypt(EMAIL));
        cert.setCertTrgtEmlHmac(emailHmac);
        cert.setCertNoHash(crypto.hmac("emlcert:" + emailHmac + ":" + CODE));
        cert.setCertFailNmtm(failCount);
        cert.setCertExpryDt(expiry);
        cert.setCertCmptnDt(completed);
        return cert;
    }

    private EmailCertification freshCert(int failCount) {
        return cert(failCount, LocalDateTime.now().plusMinutes(2), null);
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 처음_보내면_해시만_저장하고_메일_이벤트를_발행한다() {
        EmailCertificationResult result = service.send(sendRequest());

        assertThat(result.succeeded()).isTrue();
        ArgumentCaptor<EmailCertification> saved = ArgumentCaptor.forClass(EmailCertification.class);
        verify(mapper).updateInvalidatePrevious(eq(emailHmac), eq(EmailCertPurpose.SIGNUP.getCode()), any());
        verify(mapper).insert(saved.capture());
        ArgumentCaptor<CertificationMailEvent> event = ArgumentCaptor.forClass(CertificationMailEvent.class);
        verify(eventPublisher).publish(event.capture());

        Matcher code = Pattern.compile("\\d{6}").matcher(event.getValue().mailCn());
        assertThat(code.find()).isTrue();
        assertThat(saved.getValue().getCertNoHash())
                .isEqualTo(crypto.hmac("emlcert:" + emailHmac + ":" + code.group()))
                .doesNotContain(code.group());
        assertThat(event.getValue().to()).isEqualTo(EMAIL);
        assertThat(saved.getValue().getCertExpryDt()).isAfter(LocalDateTime.now().plusSeconds(170));
    }

    @Test
    void 가입된_이메일이면_발송하지_않는다() {
        doThrow(new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED)).when(memberService)
                .checkEmailAvailable(emailHmac);

        assertThatThrownBy(() -> service.send(sendRequest()))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(mapper, never()).insert(any());
        verify(eventPublisher, never()).publish(any(CertificationMailEvent.class));
    }

    @Test
    void 탈퇴_보관_이메일은_복구_안내_오류이다() {
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_AVAILABLE)).when(memberService)
                .checkEmailAvailable(emailHmac);

        assertThatThrownBy(() -> service.send(sendRequest()))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_AVAILABLE));
    }

    @Test
    void 비밀번호_재설정_목적은_아직_받지_않는다() {
        EmailCertificationCreateRequest req = new EmailCertificationCreateRequest(
                EmailCertPurpose.PASSWORD_RESET.getCode(), EMAIL);

        assertThatThrownBy(() -> service.send(req)).satisfies(e -> assertCode(e, ErrorCode.INVALID_INPUT_VALUE));
        verify(mapper, never()).insert(any());
    }

    @Test
    void 재전송_간격_안이면_재시도_가능_시각만_돌려준다() {
        LocalDateTime latest = LocalDateTime.now().minusSeconds(20);
        when(mapper.selectRegDtList(eq(emailHmac), anyString(), any())).thenReturn(List.of(latest));

        EmailCertificationResult result = service.send(sendRequest());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.error()).isEqualTo(ErrorCode.EMAIL_CERT_RESEND_NOT_YET);
        assertThat(result.response().resendAvailableDt()).isEqualTo(latest.plusSeconds(60));
        verify(mapper, never()).insert(any());
        verify(mapper, never()).updateInvalidatePrevious(anyString(), anyString(), any());
        verify(eventPublisher, never()).publish(any(CertificationMailEvent.class));
    }

    @Test
    void 집계_구간_안_발송_횟수가_차면_가장_오래된_발송이_빠지는_시각을_알려준다() {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> recent = List.of(now.minusMinutes(50), now.minusMinutes(40), now.minusMinutes(30),
                now.minusMinutes(20), now.minusMinutes(10));
        when(mapper.selectRegDtList(eq(emailHmac), anyString(), any())).thenReturn(recent);

        EmailCertificationResult result = service.send(sendRequest());

        assertThat(result.error()).isEqualTo(ErrorCode.EMAIL_CERT_RESEND_NOT_YET);
        assertThat(result.response().resendAvailableDt()).isEqualTo(recent.get(0).plusMinutes(60));
        verify(mapper, never()).insert(any());
    }

    @Test
    void 횟수가_남고_간격이_지났으면_다시_보낼_수_있다() {
        LocalDateTime now = LocalDateTime.now();
        when(mapper.selectRegDtList(eq(emailHmac), anyString(), any()))
                .thenReturn(List.of(now.minusMinutes(30), now.minusMinutes(10)));

        assertThat(service.send(sendRequest()).succeeded()).isTrue();
        verify(mapper).updateInvalidatePrevious(eq(emailHmac), eq(EmailCertPurpose.SIGNUP.getCode()), any());
    }

    @Test
    void 확인에_성공하면_완료_시각을_남기고_가입_진행_결과를_준다() {
        when(mapper.selectBySn(3L)).thenReturn(freshCert(0));

        EmailCertificationResponse response = service.verify(3L, new EmailCertificationVerifyRequest(CODE));

        assertThat(response.certRslt()).isEqualTo(EmailCertificationResponse.SIGNUP_READY);
        assertThat(response.certCmptnDt()).isNotNull();
        verify(mapper).updateCompleted(eq(3L), any());
        verify(mapper, never()).updateFailCount(anyLong());
    }

    @Test
    void 이미_확인한_인증을_다시_확인해도_완료_시각은_그대로이다() {
        LocalDateTime completed = LocalDateTime.now().minusMinutes(5);
        when(mapper.selectBySn(3L)).thenReturn(cert(0, LocalDateTime.now().minusMinutes(4), completed));

        EmailCertificationResponse response = service.verify(3L, new EmailCertificationVerifyRequest(CODE));

        assertThat(response.certCmptnDt()).isEqualTo(completed);
        verify(mapper, never()).updateCompleted(anyLong(), any());
    }

    @Test
    void 번호가_틀리면_횟수를_올리고_불일치_오류이다() {
        when(mapper.selectBySn(3L)).thenReturn(freshCert(1));

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest("000000")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_MISMATCH));
        verify(mapper).updateFailCount(3L);
        verify(mapper, never()).updateInvalidate(anyLong(), any());
        verify(mapper, never()).updateCompleted(anyLong(), any());
    }

    @Test
    void 제한_횟수만큼_틀리면_인증을_무효화한다() {
        when(mapper.selectBySn(3L)).thenReturn(freshCert(4));

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest("000000")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_FAIL_LIMIT_EXCEEDED));
        verify(mapper).updateFailCount(3L);
        verify(mapper).updateInvalidate(eq(3L), any());
    }

    @Test
    void 만료된_인증은_실패_횟수를_올리지_않는다() {
        when(mapper.selectBySn(3L)).thenReturn(cert(0, LocalDateTime.now().minusSeconds(1), null));

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest("000000")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_EXPIRED));
        verify(mapper, never()).updateFailCount(anyLong());
    }

    @Test
    void 없거나_무효화된_인증은_찾을_수_없다() {
        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest(CODE)))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_NOT_FOUND));

        EmailCertification invalidated = freshCert(0);
        invalidated.setCertInvldDt(LocalDateTime.now());
        when(mapper.selectBySn(4L)).thenReturn(invalidated);
        assertThatThrownBy(() -> service.verify(4L, new EmailCertificationVerifyRequest(CODE)))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_NOT_FOUND));
    }

    @Test
    void 다른_목적의_인증은_찾을_수_없다() {
        EmailCertification other = freshCert(0);
        other.setEmlCertPrpsCd(EmailCertPurpose.PASSWORD_RESET.getCode());
        when(mapper.selectBySn(3L)).thenReturn(other);

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest(CODE)))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_NOT_FOUND));
    }

    @Test
    void 이미_가입에_쓴_인증은_다시_확인할_수_없다() {
        EmailCertification used = cert(0, LocalDateTime.now().plusMinutes(1), LocalDateTime.now().minusMinutes(1));
        used.setCertUseCmptnDt(LocalDateTime.now());
        when(mapper.selectBySn(3L)).thenReturn(used);

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest(CODE)))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_ALREADY_USED));
    }

    @Test
    void 확인_사이에_가입된_이메일이면_완료로_바꾸지_않는다() {
        when(mapper.selectBySn(3L)).thenReturn(freshCert(0));
        doThrow(new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED)).when(memberService)
                .checkEmailAvailable(emailHmac);

        assertThatThrownBy(() -> service.verify(3L, new EmailCertificationVerifyRequest(CODE)))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(mapper, never()).updateCompleted(anyLong(), any());
    }

    @Test
    void 가입_때_인증이_확인되지_않았으면_거절한다() {
        when(mapper.selectBySn(3L)).thenReturn(freshCert(0));

        assertThatThrownBy(() -> service.checkForSignup(3L, CODE))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_NOT_VERIFIED));
    }

    @Test
    void 가입_때_인증_완료_유지시간이_지났으면_거절한다() {
        when(mapper.selectBySn(3L))
                .thenReturn(cert(0, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(31)));

        assertThatThrownBy(() -> service.checkForSignup(3L, CODE))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_EXPIRED));
    }

    @Test
    void 가입_때_번호가_틀려도_실패_횟수를_남긴다() {
        when(mapper.selectBySn(3L)).thenReturn(cert(0, LocalDateTime.now().plusMinutes(1), LocalDateTime.now()));

        assertThatThrownBy(() -> service.checkForSignup(3L, "000000"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_MISMATCH));
        verify(mapper).updateFailCount(3L);
    }

    @Test
    void 가입_때_확인된_인증은_그대로_돌려준다() {
        EmailCertification verified = cert(0, LocalDateTime.now().plusMinutes(1), LocalDateTime.now());
        when(mapper.selectBySn(3L)).thenReturn(verified);

        assertThat(service.checkForSignup(3L, CODE)).isSameAs(verified);
    }

    @Test
    void 인증은_한_번만_사용_처리된다() {
        when(mapper.updateUsed(eq(3L), any())).thenReturn(1, 0);

        service.use(3L);

        assertThatThrownBy(() -> service.use(3L)).satisfies(e -> assertCode(e, ErrorCode.ALREADY_PROCESSED));
    }
}
