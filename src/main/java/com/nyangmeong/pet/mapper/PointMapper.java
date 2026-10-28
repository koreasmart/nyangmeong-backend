package com.nyangmeong.pet.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.PointLedger;
import com.nyangmeong.pet.dto.PointLedgerSummary;
import com.nyangmeong.pet.dto.PointMonthSummary;

/**
 * [포인트 Mapper]
 * - MBR.HLD_PNT·PNT_GNLGR 조회·저장 (FN-MYP-020)
 * - MBR.HLD_PNT는 member에 service가 없어 이 Mapper만 바꿈
 */
@Mapper
public interface PointMapper {

    /**
     * 보유 포인트 조회
     */
    Integer selectHldPnt(@Param("mbrSn") Long mbrSn);

    /**
     * 보유 포인트 증감
     */
    int updateHldPnt(@Param("mbrSn") Long mbrSn, @Param("delta") int delta, @Param("actorId") String actorId);

    /**
     * 멱등 요청 ID 원장 존재 여부
     */
    boolean existsLedger(@Param("idmptDmndId") String idmptDmndId);

    /**
     * 포인트 원장 등록
     */
    int insertLedger(PointLedger ledger);

    /**
     * 기간 포인트 내역 조회
     */
    List<PointLedgerSummary> selectLedgerList(@Param("mbrSn") Long mbrSn, @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt);

    /**
     * 기간 적립·사용 합계 조회
     */
    PointMonthSummary selectMonthSummary(@Param("mbrSn") Long mbrSn, @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt);

    /**
     * 반려동물의 주양육자 조회
     */
    Long selectPrimaryMbrSn(@Param("petSn") Long petSn, @Param("primaryRoleCd") String primaryRoleCd);
}
