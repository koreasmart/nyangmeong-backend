package com.nyangmeong.member.dto;

import com.nyangmeong.global.error.ErrorCode;

/**
 * [인증번호 발송 처리 결과]
 * - 재전송 제한은 예외가 아니라 결과로 돌려준다 : 응답 data 에 재시도 가능 시각을 싣기 위해
 */
public record EmailCertificationResult(ErrorCode error, EmailCertificationResponse response) {

    public static EmailCertificationResult success(EmailCertificationResponse response) {
        return new EmailCertificationResult(null, response);
    }

    public static EmailCertificationResult fail(ErrorCode error, EmailCertificationResponse response) {
        return new EmailCertificationResult(error, response);
    }

    public boolean succeeded() {
        return error == null;
    }
}
