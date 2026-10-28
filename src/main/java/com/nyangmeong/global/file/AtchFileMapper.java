package com.nyangmeong.global.file;

import org.apache.ibatis.annotations.Mapper;

/**
 * [첨부파일 Mapper]
 */
@Mapper
public interface AtchFileMapper {

    /**
     * 첨부파일 등록
     */
    void insert(AtchFile atchFile);

    /**
     * 사용 중인 첨부파일 조회
     */
    AtchFile selectInUse(Long atchFileSn);
}
