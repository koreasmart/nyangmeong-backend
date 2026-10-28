package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.external.ReceiptOcrResponse.Drug;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;

import lombok.RequiredArgsConstructor;

/**
 * [가짜 영수증 OCR]
 * - 외부 API 결정 전 개발용 : 이미지와 무관하게 app.ocr.client 값의 고정 결과
 * - 검진일 = 오늘
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.ocr.client:fake}'.startsWith('fake')")
public class FakeOcrClient implements OcrClient {

    private final OcrProperties ocrProperties;

    @Override
    public ReceiptOcrResponse recognize(byte[] image, String contentType) {
        LocalDate today = LocalDate.now();
        return switch (ocrProperties.getClient()) {
            case "fake-fail" -> throw new IllegalStateException("가짜 OCR API 오류");
            case "fake-timeout" -> throw new OcrTimeoutException("가짜 OCR 분석 시간 초과");
            case "fake-empty" -> new ReceiptOcrResponse(null, null, null, null, List.of());
            case "fake-partial" -> new ReceiptOcrResponse("냥멍동물병원", null, 33_000L, null,
                    List.of(new Section(null, 33_000L, null,
                            List.of(new Line("진찰료", 10_000L), new Line(null, 15_000L)),
                            List.of(new Drug("아목시실린", null, "1일 2회", 8_000L)))));
            case "fake-single" -> new ReceiptOcrResponse("냥멍동물병원", today, 45_000L, null,
                    List.of(new Section(null, 45_000L, null,
                            List.of(new Line("진찰료", 10_000L), new Line("종합백신", 30_000L)),
                            List.of(new Drug("넥스가드", 30, "1일 1회", 5_000L)))));
            default -> new ReceiptOcrResponse("냥멍동물병원", today, 80_000L, 4_000L, List.of(
                    new Section("미르", 50_000L, null,
                            List.of(new Line("진찰료", 10_000L), new Line("피부검사", 25_000L)),
                            List.of(new Drug("아포퀠", 7, "1일 2회", 15_000L))),
                    new Section("초코", 34_000L, null,
                            List.of(new Line("진찰료", 10_000L), new Line("종합백신", 24_000L)),
                            List.of())));
        };
    }
}
