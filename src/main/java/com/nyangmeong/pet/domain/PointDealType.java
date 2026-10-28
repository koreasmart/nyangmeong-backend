package com.nyangmeong.pet.domain;

/**
 * [포인트 거래유형 코드]
 * - MYPG03 (PNT_GNLGR.PNT_DLNG_TYPE_CD)
 */
public enum PointDealType {
    MISSION("MYPC0007"),
    ITEM_PURCHASE("MYPC0008"),
    ADMIN_ADJUST("MYPC0009"),
    CHECKUP("MYPC0021");

    private final String code;

    PointDealType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
