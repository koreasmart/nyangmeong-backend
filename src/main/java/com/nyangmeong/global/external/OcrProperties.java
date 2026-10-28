package com.nyangmeong.global.external;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [영수증 OCR 설정]
 * - app.ocr.client : fake(2마리 영수증), fake-single(구역 없는 영수증), fake-partial(부분 인식),
 *   fake-empty(추출 0건), fake-fail(API 오류), fake-timeout(시간 초과)
 * - app.ocr.timeout : 분석 제한 시간, 기본 15초 (FN-AIM-008-07)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {

    /** OCR 클라이언트 종류 */
    private String client = "fake";

    /** 분석 제한 시간 */
    private Duration timeout = Duration.ofSeconds(15);
}
