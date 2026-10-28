package com.nyangmeong.member.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.member.domain.EmailCertPurpose;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.dto.EmailCertificationCreateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.EmailCertificationResult;
import com.nyangmeong.member.dto.EmailCertificationVerifyRequest;
import com.nyangmeong.member.mapper.EmailCertificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * [이메일 인증 서비스]
 * - 가입용 인증번호 발송·재전송·확인, 가입 때 인증 검사와 사용 처리
 * - 인증번호 원문은 저장하지 않고 이메일 HMAC 가 섞인 해시만 저장한다
 * - 비밀번호 재설정(USRC0011)·복구(USRC0012) 목적은 해당 마일스톤에서 연다
 */
@Service
@RequiredArgsConstructor
public class EmailCertificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MAIL_SUBJECT = "[냥멍케어] 이메일 인증번호 안내";

    private final EmailCertificationMapper emailCertificationMapper;
    private final MemberService memberService;
    private final FieldCryptoService fieldCryptoService;
    private final EventPublisher eventPublisher;
    private final EmailCertProperties properties;

    /**
     * 인증번호 발송·재전송 : 이전 번호는 무효, 간격·횟수 제한에 걸리면 재시도 가능 시각을 돌려준다
     */
    @Transactional
    public EmailCertificationResult send(EmailCertificationCreateRequest req) {
        if (!EmailCertPurpose.SIGNUP.is(req.emlCertPrpsCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String email = fieldCryptoService.normalizeEmail(req.certTrgtEml());
        String emailHmac = fieldCryptoService.hmac(email);
        memberService.checkEmailAvailable(emailHmac);

        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> recent = emailCertificationMapper.selectRegDtList(emailHmac,
                EmailCertPurpose.SIGNUP.getCode(), now.minusMinutes(properties.getResendWindowMinutes()));
        LocalDateTime resendAvailableDt = resendAvailableAt(recent, now);
        if (resendAvailableDt != null) {
            return EmailCertificationResult.fail(ErrorCode.EMAIL_CERT_RESEND_NOT_YET,
                    EmailCertificationResponse.resendLimited(resendAvailableDt));
        }

        emailCertificationMapper.updateInvalidatePrevious(emailHmac, EmailCertPurpose.SIGNUP.getCode(), now);
        String certNo = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailCertification cert = new EmailCertification();
        cert.setEmlCertPrpsCd(EmailCertPurpose.SIGNUP.getCode());
        cert.setCertTrgtEmlEnc(fieldCryptoService.encrypt(email));
        cert.setCertTrgtEmlHmac(emailHmac);
        cert.setCertNoHash(certNoHash(emailHmac, certNo));
        cert.setCertExpryDt(now.plusSeconds(properties.getCodeValiditySeconds()));
        cert.setRegDt(now);
        emailCertificationMapper.insert(cert);

        long minutes = Math.max(1, (properties.getCodeValiditySeconds() + 59) / 60);
        eventPublisher.publish(new CertificationMailEvent(email, MAIL_SUBJECT,
                "인증번호는 " + certNo + " 입니다. " + minutes + "분 안에 입력해 주세요."));
        return EmailCertificationResult.success(EmailCertificationResponse.sent(cert));
    }

    /**
     * 인증번호 확인 : 틀린 횟수는 오류로 끝나도 남아야 해서 CustomException 에도 롤백하지 않는다
     */
    @Transactional(noRollbackFor = CustomException.class)
    public EmailCertificationResponse verify(Long emlCertSn, EmailCertificationVerifyRequest req) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !EmailCertPurpose.SIGNUP.is(cert.getEmlCertPrpsCd()) || cert.getCertInvldDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_FOUND);
        }
        if (cert.getCertUseCmptnDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isCompleted = cert.getCertCmptnDt() != null;
        if (isCompleted ? isVerifiedExpired(cert, now) : now.isAfter(cert.getCertExpryDt())) {
            throw new CustomException(ErrorCode.EMAIL_CERT_EXPIRED);
        }
        checkCertNo(cert, req.certNo(), now);
        memberService.checkEmailAvailable(cert.getCertTrgtEmlHmac());

        if (!isCompleted) {
            emailCertificationMapper.updateCompleted(emlCertSn, now);
        }
        return EmailCertificationResponse.verified(cert, isCompleted ? cert.getCertCmptnDt() : now);
    }

    /**
     * 가입 때 인증 검사 : 별도 트랜잭션이라 번호가 틀려 가입이 롤백돼도 실패 횟수는 남는다
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CustomException.class)
    public EmailCertification checkForSignup(Long emlCertSn, String certNo) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !EmailCertPurpose.SIGNUP.is(cert.getEmlCertPrpsCd()) || cert.getCertInvldDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        checkCertNo(cert, certNo, now);
        if (cert.getCertCmptnDt() == null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_VERIFIED);
        }
        /** 이미 쓴 인증은 호출한 쪽이 같은 가입의 재요청인지 가린다 */
        if (cert.getCertUseCmptnDt() == null && isVerifiedExpired(cert, now)) {
            throw new CustomException(ErrorCode.EMAIL_CERT_EXPIRED);
        }
        return cert;
    }

    /**
     * 인증 사용 처리 : 한 번만, 먼저 쓴 요청이 있으면 처리 중 오류
     */
    @Transactional
    public void use(Long emlCertSn) {
        if (emailCertificationMapper.updateUsed(emlCertSn, LocalDateTime.now()) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
    }

    /** 틀리면 횟수를 올리고, 제한에 닿으면 그 인증을 무효화 */
    private void checkCertNo(EmailCertification cert, String certNo, LocalDateTime now) {
        if (certNoHash(cert.getCertTrgtEmlHmac(), certNo).equals(cert.getCertNoHash())) {
            return;
        }
        emailCertificationMapper.updateFailCount(cert.getEmlCertSn());
        if (cert.getCertFailNmtm() + 1 >= properties.getFailLimit()) {
            emailCertificationMapper.updateInvalidate(cert.getEmlCertSn(), now);
            throw new CustomException(ErrorCode.EMAIL_CERT_FAIL_LIMIT_EXCEEDED);
        }
        throw new CustomException(ErrorCode.EMAIL_CERT_MISMATCH);
    }

    /** 인증 완료 결과를 가입에 쓸 수 있는 시간이 지났는지 */
    private boolean isVerifiedExpired(EmailCertification cert, LocalDateTime now) {
        return now.isAfter(cert.getCertCmptnDt().plusMinutes(properties.getVerifiedKeepMinutes()));
    }

    /** 다시 보낼 수 있는 시각, 바로 보낼 수 있으면 null */
    private LocalDateTime resendAvailableAt(List<LocalDateTime> recent, LocalDateTime now) {
        LocalDateTime availableAt = null;
        if (!recent.isEmpty()) {
            LocalDateTime byInterval = recent.get(recent.size() - 1).plusSeconds(properties.getResendIntervalSeconds());
            if (now.isBefore(byInterval)) {
                availableAt = byInterval;
            }
        }
        if (recent.size() >= properties.getResendMaxCount()) {
            LocalDateTime byCount = recent.get(recent.size() - properties.getResendMaxCount())
                    .plusMinutes(properties.getResendWindowMinutes());
            if (availableAt == null || byCount.isAfter(availableAt)) {
                availableAt = byCount;
            }
        }
        return availableAt;
    }

    private String certNoHash(String emailHmac, String certNo) {
        return fieldCryptoService.hmac("emlcert:" + emailHmac + ":" + certNo);
    }
}
