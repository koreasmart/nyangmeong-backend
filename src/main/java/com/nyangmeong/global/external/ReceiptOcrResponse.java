package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.util.List;

/**
 * [영수증 OCR 추출값]
 * - 읽지 못한 칸은 null, 목록은 빈 목록
 * - 반려동물 구역이 없는 영수증 -> sectionNm null 구역 1개
 */
public record ReceiptOcrResponse(String hsptlNm, LocalDate ckupYmd, Long recptAmt, Long btmDscntAmt,
        List<Section> sections) {

    public ReceiptOcrResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    /**
     * [반려동물 구역]
     * - dscntAmt : 구역 안에 찍힌 할인
     */
    public record Section(String sectionNm, Long sbttlAmt, Long dscntAmt, List<Line> items, List<Drug> drugs) {

        public Section {
            items = items == null ? List.of() : List.copyOf(items);
            drugs = drugs == null ? List.of() : List.copyOf(drugs);
        }
    }

    /**
     * [진료 품목]
     */
    public record Line(String itemNm, Long itemAmt) {
    }

    /**
     * [처방 약품]
     * - mdcnUsage : 용법 원문 (예: 1일 2회)
     */
    public record Drug(String mdcnNm, Integer mdcnDays, String mdcnUsage, Long mdcnAmt) {
    }
}
