package com.nyangmeong.support.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [문의 첨부사진 VO]
 * - INQRY_ATCH_PHOTO 1:1
 */
@Data
public class InquiryPhoto {
    private Long inqryAtchPhotoSn;
    private Long otoInqrySn;
    private Long atchFileSn;
    private Integer sortSeq;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
