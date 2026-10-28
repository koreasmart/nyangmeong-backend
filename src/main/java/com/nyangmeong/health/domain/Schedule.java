package com.nyangmeong.health.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일정 VO]
 * - SCHDL 1:1
 */
@Data
public class Schedule {

    /** 일정일련번호 */
    private Long schdlSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 일정종류공통코드(HLTG19) */
    private String schdlKndCd;
    /** 캘린더뱃지색상공통코드(HLTG21) */
    private String cldrBadgeColrCd;
    /** 일정출처공통코드(HLTG22) */
    private String schdlSrcCd;
    /** 일정일시 */
    private LocalDateTime schdlDt;
    /** 일정명 */
    private String schdlNm;
    /** 메모내용 */
    private String memoCn;
    /** 삭제여부 */
    private String delYn;
    /** 시간입력여부 */
    private String hrInptYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
    /** 병원명 */
    private String hsptlNm;
    /** 검진유형공통코드(HLTG01) */
    private String ckupTypeCd;
}
