package com.nyangmeong.member.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [공통 인증 설정]
 * - app.email-cert.* , 키가 없는 PC 대비 기본값 (SYS_STNG 에는 두지 않는다)
 * - 코드 유효시간·재전송 제한·인증 완료 유지시간·실패 제한을 화면마다 따로 두지 않고 여기서 읽는다
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email-cert")
public class EmailCertProperties {

    /** 인증번호 유효시간(초) */
    private long codeValiditySeconds = 180;

    /** 재전송 최소 간격(초) */
    private long resendIntervalSeconds = 60;

    /** 재전송 횟수 집계 구간(분) */
    private long resendWindowMinutes = 60;

    /** 집계 구간 안 최대 발송 횟수 */
    private int resendMaxCount = 5;

    /** 인증 완료 결과를 가입에 쓸 수 있는 시간(분) */
    private long verifiedKeepMinutes = 30;

    /** 인증번호를 이 횟수만큼 틀리면 그 인증을 무효화 */
    private int failLimit = 5;
}
