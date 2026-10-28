package com.nyangmeong.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.domain.Terms;
import com.nyangmeong.member.dto.TermsSummary;

/**
 * [약관 Mapper]
 */
@Mapper
public interface TermsMapper {

    /**
     * 사용 중인 약관 목록 (정렬순서)
     */
    List<TermsSummary> selectUsableList();

    /**
     * 사용 중인 약관 전문, 없으면 null
     */
    Terms selectUsableBySn(Long trmsSn);

    /**
     * 회원 약관 동의 등록
     */
    int insertAgreement(@Param("mbrSn") Long mbrSn, @Param("trmsSn") Long trmsSn, @Param("agreYn") String agreYn,
            @Param("actorId") String actorId);
}
