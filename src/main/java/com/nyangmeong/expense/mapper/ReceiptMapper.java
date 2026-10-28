package com.nyangmeong.expense.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;

/**
 * [영수증 Mapper]
 * - RECPT, RECPT_STTS_HSTRY
 */
@Mapper
public interface ReceiptMapper {

    /**
     * 회원 영수증 조회
     */
    Receipt selectOwned(@Param("recptSn") Long recptSn, @Param("mbrSn") Long mbrSn);

    /**
     * 영수증 등록
     */
    void insert(Receipt receipt);

    /**
     * 상태 변경
     */
    int updateStatus(@Param("recptSn") Long recptSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 분석 종료 상태와 OCR 응답 저장
     */
    int updateAnalyzed(@Param("recptSn") Long recptSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("ocrRspnsJson") String ocrRspnsJson);

    /**
     * 멱등 요청 ID 로 영수증 조회
     */
    Receipt selectByIdmptDmndId(@Param("idmptDmndId") String idmptDmndId);

    /**
     * 저장 완료 처리 : 상태, 저장 일시, 멱등 요청 ID
     */
    int updateSaved(@Param("recptSn") Long recptSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("idmptDmndId") String idmptDmndId);

    /**
     * 상태 이력 등록
     */
    void insertStatusHistory(ReceiptStatusHistory history);
}
