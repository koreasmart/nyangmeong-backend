package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일정 목록 행]
 * - 월간 캘린더·날짜 상세·다가오는 일정 (FN-HLT-003-29·31·40)
 */
@Data
public class CareScheduleSummary {

    /** 일정일련번호 */
    private Long schdlSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 일정종류공통코드 */
    private String schdlKndCd;
    /** 일정종류명 */
    private String schdlKndCdNm;
    /** 일정일시 */
    private LocalDateTime schdlDt;
    /** 시간입력여부 */
    private boolean hrInptYn;
    /** 검진유형공통코드 */
    private String ckupTypeCd;
    /** 검진유형명 */
    private String ckupTypeCdNm;
    /** 병원명 */
    private String hsptlNm;
    /** 일정명 */
    private String schdlNm;
    /** 메모내용 */
    private String memoCn;
    /** 캘린더뱃지색상공통코드 */
    private String cldrBadgeColrCd;
    /** 연결 검진기록 수 */
    private Integer ckupRcdCnt;
}
