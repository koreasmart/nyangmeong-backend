package com.nyangmeong.global.security.session;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.global.security.domain.RefreshToken;

/**
 * [Refresh 토큰 Mapper]
 * - global 전용 Mapper, 해시만 저장
 */
@Mapper
public interface RefreshTokenMapper {

    /**
     * 세션 행 등록
     */
    void insert(@Param("token") RefreshToken token, @Param("expirySeconds") long expirySeconds);

    /**
     * 토큰 해시 갱신
     */
    void updateHash(@Param("rfrshTknSn") Long rfrshTknSn, @Param("rfrshTknHash") String rfrshTknHash);

    /**
     * 유효한 세션 조회
     */
    RefreshToken selectActiveByHash(String rfrshTknHash);

    /**
     * 세션 폐기
     */
    int updateDiscard(Long rfrshTknSn);

    /**
     * 해시로 세션 폐기
     */
    int updateDiscardByHash(String rfrshTknHash);
}
