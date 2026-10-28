package com.nyangmeong.community.domain;

/**
 * [게시판 코드]
 * - CMUG01 (PST.BBS_CD)
 */
public enum BoardType {
    PET_INFO("CMUC0001"),
    PET_DAILY("CMUC0002");

    private final String code;

    BoardType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 허용 코드 여부
     */
    public static boolean exists(String code) {
        for (BoardType type : values()) {
            if (type.is(code)) {
                return true;
            }
        }
        return false;
    }
}
