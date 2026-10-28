package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nyangmeong.member.domain.EmailCertification;

/**
 * [이메일 인증 응답]
 * - 발송 : 만료시각 / 재전송 제한 : 재시도 가능 시각만 / 확인 : 인증 완료 시각과 결과(SIGNUP_READY)
 * - 비밀번호 재설정·복구용 필드는 해당 마일스톤에서 추가
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailCertificationResponse(Long emlCertSn, String emlCertPrpsCd, LocalDateTime certExpryDt,
        LocalDateTime resendAvailableDt, LocalDateTime certCmptnDt, String certRslt) {

    public static final String SIGNUP_READY = "SIGNUP_READY";

    public static EmailCertificationResponse sent(EmailCertification cert) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, null, null);
    }

    public static EmailCertificationResponse resendLimited(LocalDateTime resendAvailableDt) {
        return new EmailCertificationResponse(null, null, null, resendAvailableDt, null, null);
    }

    public static EmailCertificationResponse verified(EmailCertification cert, LocalDateTime certCmptnDt) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, certCmptnDt, SIGNUP_READY);
    }
}
