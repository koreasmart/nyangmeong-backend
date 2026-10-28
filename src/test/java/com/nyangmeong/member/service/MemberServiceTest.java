package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 회원 서비스]
 * - 이메일·번호·닉네임 중복 판단, 회원 저장 값, 닉네임 설정
 */
class MemberServiceTest {

    private static final String EMAIL = "newbie@example.com";
    private static final String PHONE = "01012345678";

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final FieldCryptoService crypto = cryptoService();
    private final MemberService service = new MemberService(memberMapper, crypto);

    /** Mockito 는 Long 반환을 0으로 채우므로 "쓰는 회원 없음"을 null 로 명시 */
    @BeforeEach
    void stubNoOwner() {
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(null);
        when(memberMapper.selectMbrSnByPhoneHmac(anyString())).thenReturn(null);
    }

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private Member stored(Long mbrSn, String statusCode, String ncnm, String ncnmStngCmptnYn) {
        Member member = new Member();
        member.setMbrSn(mbrSn);
        member.setMbrSttsCd(statusCode);
        member.setJoinPathCd(JoinPath.EMAIL.getCode());
        member.setAcntEmlEnc(crypto.encrypt(EMAIL));
        member.setMblTelnoEnc(crypto.encrypt(PHONE));
        member.setMbrNcnm(ncnm);
        member.setNcnmStngCmptnYn(ncnmStngCmptnYn);
        return member;
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private void givenInsertAssignsSn(long mbrSn) {
        doAnswer(invocation -> {
            invocation.<Member>getArgument(0).setMbrSn(mbrSn);
            return 1;
        }).when(memberMapper).insert(any());
    }

    @Test
    void 없는_이메일은_가입할_수_있다() {
        service.checkEmailAvailable(crypto.emailHmac(EMAIL));
    }

    @Test
    void 정상_회원_이메일은_중복이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.NORMAL.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void 정지_회원_이메일도_중복이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.SUSPENDED.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void 탈퇴_보관_회원_이메일은_복구_안내이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.WITHDRAWN.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_AVAILABLE));
    }

    @Test
    void 휴대전화번호는_정규화해_중복을_확인한다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThat(service.getPhoneAvailability("+82 10-1234-5678", null).normalizedMblTelno()).isEqualTo(PHONE);
        assertThat(service.getPhoneAvailability("010-1234-5678", null).available()).isFalse();
        assertThat(service.getPhoneAvailability("010-1234-5678", 6L).available()).isFalse();
    }

    @Test
    void 로그인_회원_본인의_번호는_사용_가능하다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThat(service.getPhoneAvailability("01012345678", 5L).available()).isTrue();
    }

    @Test
    void 쓰는_사람이_없는_번호는_사용_가능하다() {
        assertThat(service.getPhoneAvailability("01012345678", null).available()).isTrue();
    }

    @Test
    void 형식이_틀린_번호는_거절한다() {
        assertThatThrownBy(() -> service.getPhoneAvailability("02-123-4567", null))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_PHONE_NUMBER));
    }

    @Test
    void 가입_때_다른_회원이_쓰는_번호는_거절한다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThatThrownBy(() -> service.checkPhoneAvailable(PHONE))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
    }

    @Test
    void 닉네임은_다른_회원이_쓰면_사용할_수_없다() {
        when(memberMapper.selectMbrSnByNickname("초코맘")).thenReturn(5L);

        assertThat(service.getNicknameAvailability("초코맘", null).available()).isFalse();
        assertThat(service.getNicknameAvailability("초코맘", 6L).available()).isFalse();
        assertThat(service.getNicknameAvailability("초코맘", 5L).available()).isTrue();
        assertThat(service.getNicknameAvailability("새닉네임", null).available()).isTrue();
    }

    @Test
    void 가입하면_정상_일반회원으로_저장하고_최초_상태_이력을_남긴다() {
        givenInsertAssignsSn(11L);

        Member created = service.create(EMAIL, PHONE, "hash");

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insert(saved.capture());
        Member member = saved.getValue();
        assertThat(created.getMbrSn()).isEqualTo(11L);
        assertThat(member.getMbrSttsCd()).isEqualTo(MemberStatus.NORMAL.getCode());
        assertThat(member.getMbrAuthrtCd()).isEqualTo(MemberAuthority.USER.getCode());
        assertThat(member.getJoinPathCd()).isEqualTo(JoinPath.EMAIL.getCode());
        assertThat(member.getPswdHash()).isEqualTo("hash");
        assertThat(member.getAcntEmlHmac()).isEqualTo(crypto.emailHmac(EMAIL));
        assertThat(crypto.decrypt(member.getAcntEmlEnc())).isEqualTo(EMAIL);
        assertThat(member.getMblTelnoHmac()).isEqualTo(crypto.hmac(PHONE));
        assertThat(crypto.decrypt(member.getMblTelnoEnc())).isEqualTo(PHONE);
        assertThat(member.getLastLgnDt()).isNull();
        verify(memberMapper).insertStatusHistory(11L, null, MemberStatus.NORMAL.getCode(), "회원가입");
    }

    @Test
    void 기본_닉네임은_한글_숫자_10글자이다() {
        givenInsertAssignsSn(11L);

        String nickname = service.create(EMAIL, PHONE, "hash").getMbrNcnm();

        assertThat(nickname).matches("^냥멍\\d{8}$").hasSize(10);
    }

    @Test
    void 기본_닉네임이_겹치면_다시_만든다() {
        givenInsertAssignsSn(11L);
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(1L, 1L, null);

        assertThat(service.create(EMAIL, PHONE, "hash").getMbrNcnm()).startsWith("냥멍");
        verify(memberMapper, org.mockito.Mockito.times(3)).selectMbrSnByNickname(anyString());
    }

    @Test
    void 기본_닉네임을_계속_못_만들면_서버_오류이다() {
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.INTERNAL_SERVER_ERROR));
        verify(memberMapper, never()).insert(any());
    }

    @Test
    void 동시_가입으로_유일_키가_부딪히면_키_이름으로_오류를_가른다() {
        when(memberMapper.insert(any())).thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_1'"))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_3'"))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_2'"));

        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.NICKNAME_ALREADY_USED));
        verify(memberMapper, never()).insertStatusHistory(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void 닉네임_설정은_저장과_완료_표시를_함께_한다() {
        Member member = stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N");
        Member updated = stored(5L, MemberStatus.NORMAL.getCode(), "초코맘", "Y");
        when(memberMapper.selectBySn(5L)).thenReturn(member, updated);
        when(memberMapper.selectSnsOffrCds(5L)).thenReturn(List.of());

        MemberResponse response = service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘"));

        verify(memberMapper).updateNickname(5L, "초코맘", "5");
        assertThat(response.mbrNcnm()).isEqualTo("초코맘");
        assertThat(response.ncnmStngCmptnYn()).isTrue();
        assertThat(response.acntEml()).isEqualTo(EMAIL);
        assertThat(response.mblTelno()).isEqualTo(PHONE);
    }

    @Test
    void 기본_닉네임_그대로도_확정할_수_있다() {
        Member member = stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N");
        when(memberMapper.selectBySn(5L)).thenReturn(member);
        when(memberMapper.selectMbrSnByNickname("냥멍12345678")).thenReturn(5L);

        service.updateNickname(5L, new MemberNicknameUpdateRequest("냥멍12345678"));

        verify(memberMapper).updateNickname(5L, "냥멍12345678", "5");
    }

    @Test
    void 다른_회원이_쓰는_닉네임은_저장하지_않는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N"));
        when(memberMapper.selectMbrSnByNickname("초코맘")).thenReturn(9L);

        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.NICKNAME_ALREADY_USED));
        verify(memberMapper, never()).updateNickname(anyLong(), anyString(), anyString());
    }

    @Test
    void 정지_탈퇴_회원의_닉네임은_바꾸지_않는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.WITHDRAWN.getCode(), "a", "N"));

        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_INACTIVE));
        verify(memberMapper, never()).updateNickname(anyLong(), anyString(), anyString());
    }

    @Test
    void 없는_회원의_닉네임은_바꾸지_않는다() {
        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 회원_응답은_이메일과_번호를_복호화하고_SNS_연동을_담는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.NORMAL.getCode(), "초코맘", "Y"));
        when(memberMapper.selectSnsOffrCds(5L)).thenReturn(List.of(JoinPath.KAKAO.getCode()));

        MemberResponse response = service.getMember(5L);

        assertThat(response.snsAcntLinks()).hasSize(1);
        assertThat(response.snsAcntLinks().get(0).snsOffrCd()).isEqualTo(JoinPath.KAKAO.getCode());
        assertThat(response.joinPathCd()).isEqualTo(JoinPath.EMAIL.getCode());
        verify(memberMapper).selectSnsOffrCds(eq(5L));
    }
}
