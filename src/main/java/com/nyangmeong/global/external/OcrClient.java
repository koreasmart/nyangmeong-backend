package com.nyangmeong.global.external;

/**
 * [영수증 OCR 외부 API]
 * - 제한 시간 초과 -> OcrTimeoutException, 그 밖의 API 오류 -> RuntimeException (FN-AIM-008-07·010-01)
 * - 외부 API 규격 미정 -> FakeOcrClient
 */
public interface OcrClient {

    /**
     * 영수증 항목 추출
     */
    ReceiptOcrResponse recognize(byte[] image, String contentType);
}
