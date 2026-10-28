package com.nyangmeong.member.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.validation.PhoneNumberNormalizer;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberNicknameAvailabilityResponse;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberPhoneAvailabilityResponse;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberSnsAccountSummary;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원 서비스]
 * - 다른 도메인(auth 등)은 mapper 대신 이 서비스를 부른다
 * - 가입 때 이메일·번호·닉네임 중복 검사와 회원 저장, 닉네임 설정
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String NICKNAME_PREFIX = "냥멍";
    private static final int NICKNAME_MAX_TRY = 5;
    private static final String SIGNUP_REASON = "회원가입";

    private final MemberMapper memberMapper;
    private final FieldCryptoService fieldCryptoService;

    /**
     * 로그인용 회원 조회 : 없으면 null (없는 아이디도 불일치와 같게 다루기 위해)
     */
    @Transactional(readOnly = true)
    public Member findForLogin(String acntEmlHmac) {
        return memberMapper.selectByEmailHmac(acntEmlHmac);
    }

    /**
     * 로그인 성공 기록 (최종 로그인 일시)
     */
    @Transactional
    public void recordLogin(Long mbrSn) {
        memberMapper.updateLastLogin(mbrSn, String.valueOf(mbrSn));
    }

    /**
     * 가입 이메일 사용 가능 검사 : 정상·정지 회원은 중복, 탈퇴 보관 회원은 복구 안내
     * - 실패 횟수를 남기는 호출자의 트랜잭션이 롤백 표시되지 않도록 CustomException 에는 롤백하지 않는다
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public void checkEmailAvailable(String acntEmlHmac) {
        Member member = memberMapper.selectByEmailHmac(acntEmlHmac);
        if (member == null) {
            return;
        }
        if (MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_AVAILABLE);
        }
        throw new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    /**
     * 휴대전화번호 정규화와 중복확인 : 로그인 회원 본인의 번호는 사용 가능
     */
    @Transactional(readOnly = true)
    public MemberPhoneAvailabilityResponse getPhoneAvailability(String mblTelno, Long actorSn) {
        String normalized = PhoneNumberNormalizer.normalize(mblTelno);
        Long owner = memberMapper.selectMbrSnByPhoneHmac(fieldCryptoService.hmac(normalized));
        return new MemberPhoneAvailabilityResponse(normalized, owner == null || owner.equals(actorSn));
    }

    /**
     * 가입 휴대전화번호 사용 가능 검사 (정규화된 번호)
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public void checkPhoneAvailable(String normalizedMblTelno) {
        if (memberMapper.selectMbrSnByPhoneHmac(fieldCryptoService.hmac(normalizedMblTelno)) != null) {
            throw new CustomException(ErrorCode.PHONE_ALREADY_USED);
        }
    }

    /**
     * 닉네임 중복확인 : 현재 본인 닉네임은 사용 가능
     */
    @Transactional(readOnly = true)
    public MemberNicknameAvailabilityResponse getNicknameAvailability(String mbrNcnm, Long actorSn) {
        Long owner = memberMapper.selectMbrSnByNickname(mbrNcnm);
        return new MemberNicknameAvailabilityResponse(owner == null || owner.equals(actorSn));
    }

    /**
     * 이메일 회원 등록 : 기본 닉네임, 최초 상태 이력(없음 → 정상)과 함께 저장하고 첫 로그인 전이라 최종 로그인은 비운다
     */
    @Transactional
    public Member create(String acntEml, String normalizedMblTelno, String pswdHash) {
        Member member = new Member();
        member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
        member.setJoinPathCd(JoinPath.EMAIL.getCode());
        member.setAcntEmlEnc(fieldCryptoService.encrypt(acntEml));
        member.setAcntEmlHmac(fieldCryptoService.emailHmac(acntEml));
        member.setMblTelnoEnc(fieldCryptoService.encrypt(normalizedMblTelno));
        member.setMblTelnoHmac(fieldCryptoService.hmac(normalizedMblTelno));
        member.setPswdHash(pswdHash);
        member.setMbrNcnm(createDefaultNickname());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw duplicateException(e);
        }
        memberMapper.insertStatusHistory(member.getMbrSn(), null, MemberStatus.NORMAL.getCode(), SIGNUP_REASON);
        return member;
    }

    /**
     * 회원 응답 (복호화한 이메일·번호)
     */
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long mbrSn) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return toResponse(member);
    }

    /**
     * 닉네임 설정 완료 : 저장과 설정 완료 표시를 함께, 현재 닉네임 그대로도 확정
     */
    @Transactional
    public MemberResponse updateNickname(Long mbrSn, MemberNicknameUpdateRequest req) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (MemberStatus.isInactive(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        Long owner = memberMapper.selectMbrSnByNickname(req.mbrNcnm());
        if (owner != null && !owner.equals(mbrSn)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_USED);
        }
        try {
            memberMapper.updateNickname(mbrSn, req.mbrNcnm(), String.valueOf(mbrSn));
        } catch (DuplicateKeyException e) {
            throw duplicateException(e);
        }
        return toResponse(memberMapper.selectBySn(mbrSn));
    }

    /**
     * 회원 VO를 응답으로 변환
     */
    @Transactional(readOnly = true)
    public MemberResponse toResponse(Member member) {
        List<MemberSnsAccountSummary> snsLinks = memberMapper.selectSnsOffrCds(member.getMbrSn()).stream()
                .map(MemberSnsAccountSummary::new)
                .toList();
        return new MemberResponse(fieldCryptoService.decrypt(member.getAcntEmlEnc()), member.getMbrNcnm(),
                fieldCryptoService.decrypt(member.getMblTelnoEnc()), member.getAtchFileSn(), member.getJoinPathCd(),
                "Y".equals(member.getNcnmStngCmptnYn()), snsLinks);
    }

    /** 냥멍 + 숫자 8자리(10글자), 중복이면 다시 만든다 */
    private String createDefaultNickname() {
        for (int i = 0; i < NICKNAME_MAX_TRY; i++) {
            String nickname = NICKNAME_PREFIX + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            if (memberMapper.selectMbrSnByNickname(nickname) == null) {
                return nickname;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /** 동시 요청으로 유일 키가 부딪힌 경우 : 키 이름으로 어느 값인지 가른다 */
    private CustomException duplicateException(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("UK_MBR_1")) {
            return new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (message.contains("UK_MBR_3")) {
            return new CustomException(ErrorCode.PHONE_ALREADY_USED);
        }
        return new CustomException(ErrorCode.NICKNAME_ALREADY_USED);
    }

    /**
     * 탈퇴 보관일수가 지난 회원 번호 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> searchExpiredWithdrawn(int keepDayCnt) {
        return memberMapper.selectExpiredWithdrawnMbrSns(MemberStatus.WITHDRAWN.getCode(), keepDayCnt);
    }
}
