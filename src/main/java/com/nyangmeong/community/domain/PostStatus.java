package com.nyangmeong.community.domain;

/**
 * [게시글 상태 코드]
 * - CMUG02 (PST.PST_STTS_CD), 블라인드는 신고 누적 자동 처리
 */
public enum PostStatus {
    NORMAL("CMUC0003"),
    BLIND("CMUC0004"),
    DELETED("CMUC0005");

    private final String code;

    PostStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
