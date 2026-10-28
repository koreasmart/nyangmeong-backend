package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [영수증 분석 결과]
 * - failRsn : 일시 오류 사유(TIMEOUT·API_ERROR), 그 밖의 상태는 null (FN-AIM-010-07)
 * - 인식 실패·일시 오류·취소 -> sections 빈 목록
 */
public record ReceiptResponse(Long recptSn, String ocrPrcsSttsCd, String failRsn, String hsptlNm,
        LocalDate ckupYmd, Long recptAmt, Long dstrbDscntAmt, List<Section> sections) {

    /**
     * 분석 결과 없는 응답
     */
    public static ReceiptResponse empty(Long recptSn, String ocrPrcsSttsCd, String failRsn) {
        return new ReceiptResponse(recptSn, ocrPrcsSttsCd, failRsn, null, null, null, null, List.of());
    }

    /**
     * [반려동물 탭]
     * - petSn : 자동 연결된 반려동물, 연결 못 하면 null (FN-AIM-009-17)
     * - sectionAmt = sbttlAmt - dscntAmt (FN-AIM-009-20)
     */
    public record Section(String sectionNm, Long petSn, Long sbttlAmt, Long dscntAmt, Long sectionAmt,
            String ckupCn, List<Line> items, List<Prescription> prescriptions) {
    }

    /**
     * [진료 품목]
     */
    public record Line(String itemNm, Long itemAmt) {
    }

    /**
     * [처방 약품 행]
     * - mdcnAmt : 대조용, 처방약으로 저장하지 않음 (FN-AIM-009-03)
     */
    public record Prescription(String mdcnNm, LocalDate mdcnBgngYmd, LocalDate mdcnEndYmd, Integer dayMdcnNmtm,
            String prscSttsCd, Long mdcnAmt) {
    }
}
