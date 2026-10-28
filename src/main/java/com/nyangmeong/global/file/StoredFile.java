package com.nyangmeong.global.file;

/**
 * [저장된 파일 내용]
 * - FileStore.load() 결과
 */
public record StoredFile(byte[] content, String contentType, String orgnlFileNm) {
}
