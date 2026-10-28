package com.nyangmeong.member.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.validation.PhoneNumberNormalizer;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberCreateRequest;
import com.nyangmeong.member.dto.MemberJoinResult;

import lombok.RequiredArgsConstructor;

/**
 * [회원가입 서비스]
 * - 이메일 인증·약관·번호·비밀번호를 서버에서 다시 검사한 뒤 회원·약관 동의·상태 이력을 한 트랜잭션으로 저장
 * - 인증은 한 번만 쓰고, 같은 인증으로 다시 오면 닉네임 쿠키 유효시간 안에서만 기존 가입 결과를 돌려준다
 * - 가입만으로 로그인하지 않는다 (로그인 쿠키 없음)
 */
@Service
@RequiredArgsConstructor
public class MemberJoinService {

    private final EmailCertificationService emailCertificationService;
    private final MemberService memberService;
    private final TermsService termsService;
    private final FieldCryptoService fieldCryptoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailCertProperties emailCertProperties;

    /**
     * 이메일 회원가입
     */
    @Transactional
    public MemberJoinResult create(MemberCreateRequest req) {
        if (!JoinPath.EMAIL.is(req.joinPathCd())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        if (!req.pswd().equals(req.pswdCnfrm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        EmailCertification cert = emailCertificationService.checkForSignup(req.emlCertSn(), req.certNo());
        if (cert.getCertUseCmptnDt() != null) {
            return replay(cert);
        }
        Map<Long, Boolean> agreements = termsService.checkAgreements(req.trmsAgres());

        String email = fieldCryptoService.decrypt(cert.getCertTrgtEmlEnc());
        memberService.checkEmailAvailable(cert.getCertTrgtEmlHmac());
        String mblTelno = PhoneNumberNormalizer.normalize(req.mblTelno());
        memberService.checkPhoneAvailable(mblTelno);

        emailCertificationService.use(cert.getEmlCertSn());
        Member member = memberService.create(email, mblTelno, passwordEncoder.encode(req.pswd()));
        termsService.createAgreements(member.getMbrSn(), agreements);
        return new MemberJoinResult(member.getMbrSn(), memberService.toResponse(member));
    }

    /** 응답 유실·중복 클릭 재요청 : 같은 인증으로 만든 정상 이메일 회원의 결과만 다시 준다 */
    private MemberJoinResult replay(EmailCertification cert) {
        LocalDateTime replayUntil = cert.getCertUseCmptnDt().plusMinutes(emailCertProperties.getVerifiedKeepMinutes());
        Member member = memberService.findForLogin(cert.getCertTrgtEmlHmac());
        if (LocalDateTime.now().isAfter(replayUntil) || member == null
                || !MemberStatus.NORMAL.is(member.getMbrSttsCd()) || !JoinPath.EMAIL.is(member.getJoinPathCd())) {
            throw new CustomException(ErrorCode.EMAIL_CERT_ALREADY_USED);
        }
        return new MemberJoinResult(member.getMbrSn(), memberService.toResponse(member));
    }
}
