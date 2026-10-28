package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.expense.domain.PrescriptionStatus;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.CaregiverPetSummary;
import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Drug;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;

/**
 * [영수증 OCR 결과 변환]
 * - 인식 판정 (FN-AIM-010-01·05), 반려동물 탭·자동 연결 (FN-AIM-009-16·17)
 * - 하단 할인 배분 (FN-AIM-009-20), 약품 필드 변환 (FN-AIM-009-04), 검진 내용 자동 채움 (FN-AIM-009-21)
 */
final class ReceiptOcrConverter {

    static final int CKUP_CN_MAX = 500;
    private static final String ELLIPSIS = "…";
    private static final Pattern USAGE_COUNT = Pattern.compile("(\\d+)\\s*회");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ReceiptOcrConverter() {
    }

    /**
     * 인식 상태 판정
     */
    static ReceiptStatus classify(ReceiptOcrResponse ocr) {
        long lines = ocr.sections().stream().mapToLong(section -> section.items().size()).sum();
        long drugs = ocr.sections().stream().mapToLong(section -> section.drugs().size()).sum();
        boolean hasHeader = ocr.hsptlNm() != null || ocr.ckupYmd() != null || ocr.recptAmt() != null;
        if (!hasHeader && lines == 0 && drugs == 0) {
            return ReceiptStatus.NOT_RECOGNIZED;
        }
        boolean completeHeader = ocr.hsptlNm() != null && ocr.ckupYmd() != null && ocr.recptAmt() != null;
        boolean completeLines = lines > 0 && ocr.sections().stream()
                .flatMap(section -> section.items().stream())
                .allMatch(line -> line.itemNm() != null);
        boolean completeDrugs = ocr.sections().stream()
                .flatMap(section -> section.drugs().stream())
                .allMatch(drug -> drug.mdcnNm() != null && drug.mdcnDays() != null
                        && parseDailyCount(drug.mdcnUsage()) != null);
        return completeHeader && completeLines && completeDrugs ? ReceiptStatus.RECOGNIZED : ReceiptStatus.PARTIAL;
    }

    /**
     * 확인 화면 응답 변환
     */
    static ReceiptResponse toResponse(Long recptSn, ReceiptStatus status, ReceiptOcrResponse ocr, Long petSn,
            List<CaregiverPetSummary> pets, LocalDate today) {
        List<Section> sections = ocr.sections().isEmpty()
                ? List.of(new Section(null, null, null, List.of(), List.of()))
                : ocr.sections();
        long[] dstrbDscnts = distributeDiscount(sections, ocr.btmDscntAmt());
        Set<Long> linkedPetSns = new HashSet<>();

        List<ReceiptResponse.Section> tabs = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            Long linkedPetSn = section.sectionNm() == null && sections.size() == 1
                    ? petSn
                    : matchPet(section.sectionNm(), pets, linkedPetSns);
            if (linkedPetSn != null) {
                linkedPetSns.add(linkedPetSn);
            }
            Long dscntAmt = sumOrNull(section.dscntAmt(), dstrbDscnts[i]);
            Long sectionAmt = section.sbttlAmt() == null ? null
                    : section.sbttlAmt() - (dscntAmt == null ? 0 : dscntAmt);
            tabs.add(new ReceiptResponse.Section(section.sectionNm(), linkedPetSn, section.sbttlAmt(), dscntAmt,
                    sectionAmt, toCkupCn(section.items()),
                    section.items().stream().map(line -> new ReceiptResponse.Line(line.itemNm(), line.itemAmt()))
                            .toList(),
                    section.drugs().stream().map(drug -> toPrescription(drug, ocr.ckupYmd(), today)).toList()));
        }
        Long dstrbDscntAmt = ocr.btmDscntAmt() != null && ocr.btmDscntAmt() > 0 ? ocr.btmDscntAmt() : null;
        return new ReceiptResponse(recptSn, status.getCode(), null, ocr.hsptlNm(), ocr.ckupYmd(), ocr.recptAmt(),
                dstrbDscntAmt, tabs);
    }

    /**
     * 하단 할인 구역별 배분
     * - 소계 비율, 원 단위 버림, 남는 금액은 마지막 구역
     */
    static long[] distributeDiscount(List<Section> sections, Long btmDscntAmt) {
        long[] shares = new long[sections.size()];
        if (btmDscntAmt == null || btmDscntAmt <= 0 || sections.isEmpty()) {
            return shares;
        }
        long total = sections.stream().map(Section::sbttlAmt).filter(Objects::nonNull).mapToLong(Long::longValue)
                .sum();
        long distributed = 0;
        int last = sections.size() - 1;
        for (int i = 0; i < last && total > 0; i++) {
            Long sbttlAmt = sections.get(i).sbttlAmt();
            shares[i] = sbttlAmt == null ? 0 : Math.floorDiv(btmDscntAmt * sbttlAmt, total);
            distributed += shares[i];
        }
        shares[last] = btmDscntAmt - distributed;
        return shares;
    }

    /**
     * 진료 품목명 연결
     */
    static String toCkupCn(List<Line> items) {
        String ckupCn = String.join(", ", items.stream().map(Line::itemNm).filter(Objects::nonNull).toList());
        if (ckupCn.isEmpty()) {
            return null;
        }
        return ckupCn.length() > CKUP_CN_MAX ? ckupCn.substring(0, CKUP_CN_MAX - 1) + ELLIPSIS : ckupCn;
    }

    /**
     * 약품 행 변환
     */
    static ReceiptResponse.Prescription toPrescription(Drug drug, LocalDate ckupYmd, LocalDate today) {
        LocalDate endYmd = ckupYmd == null || drug.mdcnDays() == null || drug.mdcnDays() < 1
                ? null
                : ckupYmd.plusDays(drug.mdcnDays() - 1L);
        String prscSttsCd = endYmd == null ? null
                : endYmd.isBefore(today) ? PrescriptionStatus.ENDED.getCode() : PrescriptionStatus.TAKING.getCode();
        return new ReceiptResponse.Prescription(drug.mdcnNm(), ckupYmd, endYmd, parseDailyCount(drug.mdcnUsage()),
                prscSttsCd, drug.mdcnAmt());
    }

    /**
     * 용법의 1일 횟수
     */
    static Integer parseDailyCount(String mdcnUsage) {
        if (mdcnUsage == null) {
            return null;
        }
        Matcher matcher = USAGE_COUNT.matcher(mdcnUsage);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /**
     * 구역 이름과 같은 반려동물
     * - 공백 제외 일치, 이미 다른 탭에 연결된 반려동물 제외
     */
    private static Long matchPet(String sectionNm, List<CaregiverPetSummary> pets, Set<Long> linkedPetSns) {
        if (sectionNm == null) {
            return null;
        }
        String name = WHITESPACE.matcher(sectionNm).replaceAll("");
        return pets.stream()
                .filter(pet -> pet.petNm() != null && WHITESPACE.matcher(pet.petNm()).replaceAll("").equals(name))
                .map(CaregiverPetSummary::petSn)
                .filter(petSn -> !linkedPetSns.contains(petSn))
                .findFirst()
                .orElse(null);
    }

    /**
     * 구역 할인 + 배분 할인
     */
    private static Long sumOrNull(Long dscntAmt, long dstrbDscnt) {
        if (dscntAmt == null && dstrbDscnt == 0) {
            return null;
        }
        return (dscntAmt == null ? 0 : dscntAmt) + dstrbDscnt;
    }
}
