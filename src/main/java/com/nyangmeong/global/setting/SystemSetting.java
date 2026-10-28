package com.nyangmeong.global.setting;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [시스템 설정 VO]
 * - SYS_STNG 1:1, 필드 초기값 = DDL 기본값 (설정 행이 없는 DB 대비)
 */
@Data
public class SystemSetting {
    /** 시스템설정일련번호 */
    private Long sysStngSn;
    /** 반려동물최대수 */
    private Long petMaxCnt = 10L;
    /** 공동양육최대인원수 */
    private Long jntBrupMaxNope = 5L;
    /** 게시글이미지최대수 */
    private Long pstImgMaxCnt = 10L;
    /** 파일최대크기 */
    private Long fileMaxSz = 10485760L;
    /** 대시보드케어최대노출수 */
    private Long dshbdCareMaxExpsrCnt = 6L;
    /** 로그인실패제한횟수 */
    private Long lgnFailLmtNmtm = 5L;
    /** 블라인드신고누적수 */
    private Long blindDclrAcmlCnt = 5L;
    /** 실종제보만료기준일수 */
    private Long lostTpofExpryCrtrDayCnt = 30L;
    /** 초대유효일수 */
    private Long invtnVldDayCnt = 7L;
    /** 알림숨김기준일수 */
    private Long notiHideCrtrDayCnt = 7L;
    /** 탈퇴회원보관일수 */
    private Long whdwlMbrKpngDayCnt = 30L;
    /** 최저시급적용연도 */
    private String lowstHrwgAplcnYr = "2026";
    /** 최저시급금액 */
    private Long lowstHrwgAmt = 0L;
    /** AI대화턴수 */
    private Long aiCnvrsTurnCnt = 6L;
    /** AI토큰상한수 */
    private Long aiTknUplmtCnt = 4500L;
    /** AI코멘트기본내용 */
    private String aiCmtryBscCn = "오늘도 반려동물과 행복한 하루 보내세요.";
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
    /** 찾았어요자동댓글내용 */
    private String dscvAutoCmntCn = "🎉 [펫이름]가 무사히 가족 품으로 돌아왔어요!";
    /** 매칭무응답종료기준일수 */
    private Long mtchNoRspnsEndCrtrDayCnt = 14L;
}
