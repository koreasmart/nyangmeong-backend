package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.expense.domain.PrescriptionStatus;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.CaregiverPetSummary;
import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Drug;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;

/**
 * [검사 - 영수증 OCR 결과 변환]
 */
class ReceiptOcrConverterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private Section section(String sectionNm, Long sbttlAmt, List<Line> items, List<Drug> drugs) {
        return new Section(sectionNm, sbttlAmt, null, items, drugs);
    }

    @Test
    void 추출_0건이면_인식_실패() {
        ReceiptOcrResponse empty = new ReceiptOcrResponse(null, null, null, null, List.of());
        ReceiptOcrResponse emptySection = new ReceiptOcrResponse(null, null, null, null,
                List.of(section(null, null, List.of(), List.of())));

        assertThat(ReceiptOcrConverter.classify(empty)).isEqualTo(ReceiptStatus.NOT_RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(emptySection)).isEqualTo(ReceiptStatus.NOT_RECOGNIZED);
    }

    @Test
    void 필수_항목을_모두_읽으면_인식_성공_일부만이면_부분_인식() {
        Section full = section(null, 40_000L, List.of(new Line("진찰료", 10_000L)),
                List.of(new Drug("아포퀠", 7, "1일 2회", 15_000L)));
        ReceiptOcrResponse recognized = new ReceiptOcrResponse("냥멍동물병원", TODAY, 40_000L, null, List.of(full));
        ReceiptOcrResponse noDate = new ReceiptOcrResponse("냥멍동물병원", null, 40_000L, null, List.of(full));
        ReceiptOcrResponse noUsage = new ReceiptOcrResponse("냥멍동물병원", TODAY, 40_000L, null,
                List.of(section(null, 40_000L, List.of(new Line("진찰료", 10_000L)),
                        List.of(new Drug("아포퀠", 7, "용법 미상", 15_000L)))));
        ReceiptOcrResponse headerOnly = new ReceiptOcrResponse("냥멍동물병원", null, null, null, List.of());

        assertThat(ReceiptOcrConverter.classify(recognized)).isEqualTo(ReceiptStatus.RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(noDate)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(noUsage)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(headerOnly)).isEqualTo(ReceiptStatus.PARTIAL);
    }

    @Test
    void 하단_할인은_소계_비율로_버림_배분하고_나머지는_마지막_구역() {
        List<Section> sections = List.of(section("미르", 50_000L, List.of(), List.of()),
                section("초코", 34_000L, List.of(), List.of()));

        long[] shares = ReceiptOcrConverter.distributeDiscount(sections, 4_000L);

        assertThat(shares).containsExactly(2_380L, 1_620L);
    }

    @Test
    void 할인이_없거나_소계가_없으면_마지막_구역에_전부() {
        List<Section> sections = List.of(section("미르", null, List.of(), List.of()),
                section("초코", null, List.of(), List.of()));

        assertThat(ReceiptOcrConverter.distributeDiscount(sections, null)).containsExactly(0L, 0L);
        assertThat(ReceiptOcrConverter.distributeDiscount(sections, 1_000L)).containsExactly(0L, 1_000L);
    }

    @Test
    void 검진_내용은_품목명을_쉼표로_잇고_500자_넘으면_잘라_말줄임() {
        assertThat(ReceiptOcrConverter.toCkupCn(List.of(new Line("진찰료", 1L), new Line(null, 2L),
                new Line("피부검사", 3L)))).isEqualTo("진찰료, 피부검사");
        assertThat(ReceiptOcrConverter.toCkupCn(List.of())).isNull();

        String longCn = ReceiptOcrConverter.toCkupCn(List.of(new Line("가".repeat(600), 1L)));
        assertThat(longCn).hasSize(ReceiptOcrConverter.CKUP_CN_MAX).endsWith("…");
    }

    @Test
    void 약품은_검진일_일수_용법으로_복용_기간과_상태를_채운다() {
        ReceiptResponse.Prescription taking = ReceiptOcrConverter
                .toPrescription(new Drug("아포퀠", 7, "1일 2회", 15_000L), TODAY.minusDays(6), TODAY);
        ReceiptResponse.Prescription ended = ReceiptOcrConverter
                .toPrescription(new Drug("아포퀠", 7, "1일2회", null), TODAY.minusDays(7), TODAY);
        ReceiptResponse.Prescription unknown = ReceiptOcrConverter
                .toPrescription(new Drug("아포퀠", null, null, null), TODAY, TODAY);

        assertThat(taking.mdcnBgngYmd()).isEqualTo(TODAY.minusDays(6));
        assertThat(taking.mdcnEndYmd()).isEqualTo(TODAY);
        assertThat(taking.dayMdcnNmtm()).isEqualTo(2);
        assertThat(taking.prscSttsCd()).isEqualTo(PrescriptionStatus.TAKING.getCode());
        assertThat(taking.mdcnAmt()).isEqualTo(15_000L);
        assertThat(ended.mdcnEndYmd()).isEqualTo(TODAY.minusDays(1));
        assertThat(ended.prscSttsCd()).isEqualTo(PrescriptionStatus.ENDED.getCode());
        assertThat(unknown.mdcnEndYmd()).isNull();
        assertThat(unknown.dayMdcnNmtm()).isNull();
        assertThat(unknown.prscSttsCd()).isNull();
    }

    @Test
    void 구역_없는_영수증은_고른_반려동물_탭_1개() {
        ReceiptOcrResponse ocr = new ReceiptOcrResponse("냥멍동물병원", TODAY, 40_000L, null,
                List.of(section(null, 40_000L, List.of(new Line("진찰료", 10_000L)), List.of())));

        ReceiptResponse response = ReceiptOcrConverter.toResponse(3L, ReceiptStatus.RECOGNIZED, ocr, 10L,
                List.of(), TODAY);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.RECOGNIZED.getCode());
        assertThat(response.sections()).singleElement().satisfies(tab -> {
            assertThat(tab.petSn()).isEqualTo(10L);
            assertThat(tab.sectionAmt()).isEqualTo(40_000L);
            assertThat(tab.ckupCn()).isEqualTo("진찰료");
        });
    }

    @Test
    void 구역을_하나도_못_읽으면_고른_반려동물_빈_탭() {
        ReceiptOcrResponse ocr = new ReceiptOcrResponse("냥멍동물병원", null, null, null, List.of());

        ReceiptResponse response = ReceiptOcrConverter.toResponse(3L, ReceiptStatus.PARTIAL, ocr, 10L, List.of(),
                TODAY);

        assertThat(response.sections()).singleElement().satisfies(tab -> {
            assertThat(tab.petSn()).isEqualTo(10L);
            assertThat(tab.items()).isEmpty();
            assertThat(tab.prescriptions()).isEmpty();
        });
    }

    @Test
    void 구역_이름이_공백_제외_같으면_자동_연결_한_반려동물은_한_탭에만() {
        ReceiptOcrResponse ocr = new ReceiptOcrResponse("냥멍동물병원", TODAY, 80_000L, 4_000L, List.of(
                section("미 르", 50_000L, List.of(new Line("진찰료", 10_000L)), List.of()),
                section("미르", 34_000L, List.of(new Line("종합백신", 24_000L)), List.of())));
        List<CaregiverPetSummary> pets = List.of(new CaregiverPetSummary(10L, "미르"),
                new CaregiverPetSummary(11L, "초코"));

        ReceiptResponse response = ReceiptOcrConverter.toResponse(3L, ReceiptStatus.RECOGNIZED, ocr, 11L, pets,
                TODAY);

        assertThat(response.sections()).extracting(ReceiptResponse.Section::petSn).containsExactly(10L, null);
        assertThat(response.dstrbDscntAmt()).isEqualTo(4_000L);
        assertThat(response.sections().get(0).dscntAmt()).isEqualTo(2_380L);
        assertThat(response.sections().get(0).sectionAmt()).isEqualTo(47_620L);
        assertThat(response.sections().get(1).dscntAmt()).isEqualTo(1_620L);
        assertThat(response.sections().get(1).sectionAmt()).isEqualTo(32_380L);
    }

    @Test
    void 이름이_다른_구역은_고른_반려동물이어도_연결하지_않는다() {
        ReceiptOcrResponse ocr = new ReceiptOcrResponse("냥멍동물병원", TODAY, 10_000L, null,
                List.of(section("보리", 10_000L, List.of(new Line("진찰료", 10_000L)), List.of())));

        ReceiptResponse response = ReceiptOcrConverter.toResponse(3L, ReceiptStatus.RECOGNIZED, ocr, 11L,
                List.of(new CaregiverPetSummary(11L, "초코")), TODAY);

        assertThat(response.sections()).singleElement().extracting(ReceiptResponse.Section::petSn).isNull();
        assertThat(response.dstrbDscntAmt()).isNull();
    }
}
