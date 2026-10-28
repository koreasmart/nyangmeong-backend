package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [1:1 문의 검색 조건]
 * - 처리 상태 필터, 없으면 전체 (FN-CSC-003-02)
 */
@Data
public class InquirySearchCond {
    private String inqryPrcsSttsCd;
}
