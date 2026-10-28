package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.CaregiverPetSummary;
import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.global.external.OcrClient;
import com.nyangmeong.global.external.OcrProperties;
import com.nyangmeong.global.external.OcrTimeoutException;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;
import com.nyangmeong.global.file.StoredFile;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 영수증 OCR 분석 판정]
 */
class ReceiptAnalyzerTest {

    private final ReceiptService receiptService = mock(ReceiptService.class);
    private final OcrClient ocrClient = mock(OcrClient.class);
    private final CaregiverPetFake caregiverPetFake = mock(CaregiverPetFake.class);
    private final OcrProperties ocrProperties = new OcrProperties();
    private final ReceiptAnalyzer analyzer = new ReceiptAnalyzer(receiptService, ocrClient, ocrProperties,
            caregiverPetFake, JsonMapper.builder().build());

    private final MockMultipartFile image = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    @BeforeEach
    void setUp() {
        when(receiptService.create(1L, 10L, image)).thenReturn(3L);
        when(receiptService.getImage(1L, 3L)).thenReturn(new StoredFile(new byte[] {1}, "image/png", "r.png"));
        when(receiptService.finish(eq(1L), eq(3L), any(), any(), any())).thenReturn(true);
        when(caregiverPetFake.search(1L)).thenReturn(List.of(new CaregiverPetSummary(10L, "미르")));
    }

    private ReceiptOcrResponse recognized() {
        return new ReceiptOcrResponse("냥멍동물병원", LocalDate.now(), 10_000L, null,
                List.of(new Section("미르", 10_000L, null, List.of(new Line("진찰료", 10_000L)), List.of())));
    }

    @Test
    void 인식_성공이면_결과를_저장하고_탭을_돌려준다() {
        when(ocrClient.recognize(any(), eq("image/png"))).thenReturn(recognized());

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.recptSn()).isEqualTo(3L);
        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.RECOGNIZED.getCode());
        assertThat(response.failRsn()).isNull();
        assertThat(response.hsptlNm()).isEqualTo("냥멍동물병원");
        assertThat(response.sections()).singleElement().satisfies(tab -> assertThat(tab.petSn()).isEqualTo(10L));
        verify(receiptService).finish(eq(1L), eq(3L), eq(ReceiptStatus.RECOGNIZED), isNull(),
                contains("냥멍동물병원"));
    }

    @Test
    void 추출_0건이면_인식_실패와_빈_탭() {
        when(ocrClient.recognize(any(), anyString())).thenReturn(new ReceiptOcrResponse(null, null, null, null,
                List.of()));

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.NOT_RECOGNIZED.getCode());
        assertThat(response.sections()).isEmpty();
        verify(receiptService).finish(eq(1L), eq(3L), eq(ReceiptStatus.NOT_RECOGNIZED), isNull(), anyString());
    }

    @Test
    void 일부만_읽으면_부분_인식으로_읽은_값을_채운다() {
        when(ocrClient.recognize(any(), anyString())).thenReturn(new ReceiptOcrResponse("냥멍동물병원", null,
                null, null, List.of()));

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.PARTIAL.getCode());
        assertThat(response.hsptlNm()).isEqualTo("냥멍동물병원");
        assertThat(response.sections()).singleElement().satisfies(tab -> assertThat(tab.petSn()).isEqualTo(10L));
    }

    @Test
    void API_오류면_일시_오류_API_ERROR() {
        when(ocrClient.recognize(any(), anyString())).thenThrow(new IllegalStateException("500"));

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(response.failRsn()).isEqualTo("API_ERROR");
        verify(receiptService).finish(1L, 3L, ReceiptStatus.TEMPORARY_ERROR, "API_ERROR", null);
    }

    @Test
    void 클라이언트가_시간_초과를_알리면_일시_오류_TIMEOUT() {
        when(ocrClient.recognize(any(), anyString())).thenThrow(new OcrTimeoutException("timeout"));

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.failRsn()).isEqualTo("TIMEOUT");
        verify(receiptService).finish(1L, 3L, ReceiptStatus.TEMPORARY_ERROR, "TIMEOUT", null);
    }

    @Test
    void 제한_시간을_넘기면_응답을_기다리지_않고_TIMEOUT() {
        ocrProperties.setTimeout(Duration.ofMillis(100));
        when(ocrClient.recognize(any(), anyString())).thenAnswer(invocation -> {
            Thread.sleep(5_000);
            return recognized();
        });

        long started = System.nanoTime();
        ReceiptResponse response = analyzer.create(1L, 10L, image);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(response.failRsn()).isEqualTo("TIMEOUT");
        assertThat(elapsedMillis).isLessThan(3_000);
    }

    @Test
    void 분석_중_취소됐으면_결과_대신_취소_상태() {
        when(ocrClient.recognize(any(), anyString())).thenReturn(recognized());
        when(receiptService.finish(eq(1L), eq(3L), any(), any(), any())).thenReturn(false);

        ReceiptResponse response = analyzer.create(1L, 10L, image);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.CANCELED.getCode());
        assertThat(response.sections()).isEmpty();
    }

    @Test
    void 다시_분석은_상태를_되돌린_뒤_같은_이미지로_분석한다() {
        when(ocrClient.recognize(any(), anyString())).thenReturn(recognized());

        ReceiptResponse response = analyzer.analyze(1L, 3L, 10L);

        verify(receiptService).restart(1L, 3L, 10L);
        verify(receiptService).getImage(1L, 3L);
        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.RECOGNIZED.getCode());
    }
}
