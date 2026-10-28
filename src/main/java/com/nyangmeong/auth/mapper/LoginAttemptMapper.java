package com.nyangmeong.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.auth.domain.LoginAttempt;

/**
 * [로그인 시도 제한 Mapper]
 */
@Mapper
public interface LoginAttemptMapper {

    /**
     * 아이디 기준 기록 조회
     */
    LoginAttempt selectByIdHmac(String lgnIdHmac);

    /**
     * 인증 실패 누적 : 행이 없으면 만들고, 제한 횟수에 닿으면 보안코드 요구로 바꾸며, 쓴 보안코드는 비운다
     */
    int upsertFailure(@Param("lgnIdHmac") String lgnIdHmac, @Param("failLimit") int failLimit);

    /**
     * 새 보안코드 해시 저장 (보안코드를 요구하는 아이디만)
     */
    int updateCaptchaHash(@Param("lgnIdHmac") String lgnIdHmac, @Param("scrtyCdHash") String scrtyCdHash);

    /**
     * 보안코드 해시 비움 (한 번 쓴 코드는 다시 쓸 수 없다)
     */
    int clearCaptchaHash(String lgnIdHmac);

    /**
     * 기록 초기화 (로그인 성공)
     */
    int delete(String lgnIdHmac);
}
