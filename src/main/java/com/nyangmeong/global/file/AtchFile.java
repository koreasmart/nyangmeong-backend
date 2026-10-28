package com.nyangmeong.global.file;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [첨부파일 VO]
 * - ATCH_FILE 1:1
 */
@Data
public class AtchFile {
    private Long atchFileSn;
    private String fileExtnCd;
    private String orgnlFileNm;
    private String physStrgPath;
    private Long fileSz;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
