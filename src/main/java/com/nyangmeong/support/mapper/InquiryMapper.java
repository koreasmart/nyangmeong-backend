package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatusHistory;
import com.nyangmeong.support.dto.InquirySummary;

/**
 * [1:1 문의 Mapper]
 */
@Mapper
public interface InquiryMapper {

    /**
     * 내 문의 목록 조회 (처리 상태 없으면 전체)
     */
    List<InquirySummary> selectList(@Param("mbrSn") Long mbrSn, @Param("inqryPrcsSttsCd") String inqryPrcsSttsCd);

    /**
     * 문의 단건 조회
     */
    Inquiry selectInquiry(Long otoInqrySn);

    /**
     * 공통코드명 조회
     */
    String selectCodeName(String comCd);

    /**
     * 사용 중인 첨부사진 조회
     */
    List<InquiryPhoto> selectPhotos(Long otoInqrySn);

    /**
     * 문의 등록
     */
    int insertInquiry(Inquiry inquiry);

    /**
     * 상태 이력 등록
     */
    int insertStatusHistory(InquiryStatusHistory history);

    /**
     * 첨부사진 등록
     */
    int insertPhoto(InquiryPhoto photo);

    /**
     * 접수 상태 문의의 내용 수정
     */
    int updateInquiry(@Param("otoInqrySn") Long otoInqrySn, @Param("inqryTypeCd") String inqryTypeCd,
            @Param("inqryTtl") String inqryTtl, @Param("inqryCn") String inqryCn,
            @Param("receivedCd") String receivedCd);

    /**
     * 첨부사진 삭제 처리
     */
    int updatePhotosDeleted(@Param("otoInqrySn") Long otoInqrySn, @Param("atchFileSns") List<Long> atchFileSns);

    /**
     * 접수 상태 문의에 답변 등록
     */
    int updateAnswer(@Param("otoInqrySn") Long otoInqrySn, @Param("prcsMbrSn") Long prcsMbrSn,
            @Param("ansCn") String ansCn, @Param("receivedCd") String receivedCd,
            @Param("answeredCd") String answeredCd);
}
