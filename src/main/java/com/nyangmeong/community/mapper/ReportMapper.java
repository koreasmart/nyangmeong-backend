package com.nyangmeong.community.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportStatusHistory;

/**
 * [신고 Mapper]
 */
@Mapper
public interface ReportMapper {

    /**
     * 같은 회원의 같은 대상 신고 여부 조회
     */
    boolean existsReport(@Param("mbrSn") Long mbrSn, @Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn);

    /**
     * 대상별 신고 수 조회
     */
    int countReports(@Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn);

    /**
     * 신고 등록
     */
    int insertReport(Report report);

    /**
     * 신고 상태 이력 등록
     */
    int insertStatusHistory(ReportStatusHistory history);
}
