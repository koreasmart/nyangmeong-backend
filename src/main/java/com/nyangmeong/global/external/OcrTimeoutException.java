package com.nyangmeong.global.external;

/**
 * [OCR 분석 시간 초과]
 * - app.ocr.timeout 초과 시 구현체가 던짐
 */
public class OcrTimeoutException extends RuntimeException {

    public OcrTimeoutException(String message) {
        super(message);
    }
}
