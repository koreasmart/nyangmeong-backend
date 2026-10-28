package com.nyangmeong.global.code;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * [공통코드 Mapper]
 */
@Mapper
public interface CommonCodeMapper {

    /**
     * 사용 중인 그룹 코드 존재 여부
     */
    boolean existsGroup(String prntComCd);

    /**
     * 그룹의 하위 코드 목록 조회
     */
    List<CommonCodeSummary> selectChildren(String prntComCd);
}
