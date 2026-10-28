package com.nyangmeong.pet.service;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [포인트 가짜]
 * - 포인트 조회·차감(SJM, FN-MYP-020) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 보유 포인트 고정, 차감은 잔액 검사만 (실제로 줄지 않음)
 */
@Component
public class PointFake {

    static final long HLD_PNT = 8_450L;

    /**
     * 보유 포인트 조회
     */
    public long getHldPnt(Long mbrSn) {
        return HLD_PNT;
    }

    /**
     * 포인트 사용
     * - idmptDmndId 는 연결 때 PNT_GNLGR.IDMPT_DMND_ID 로 넘긴다
     */
    public void use(Long mbrSn, long amount, String idmptDmndId) {
        if (amount > getHldPnt(mbrSn)) {
            throw new CustomException(ErrorCode.POINT_NOT_ENOUGH);
        }
    }
}
