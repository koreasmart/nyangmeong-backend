package com.nyangmeong.member.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.domain.EmailCertification;

/**
 * [이메일 인증 Mapper]
 */
@Mapper
public interface EmailCertificationMapper {

    /**
     * 인증 조회, 없으면 null
     */
    EmailCertification selectBySn(Long emlCertSn);

    /**
     * 기준 시각 이후 발송 시각 목록 (오래된 순)
     */
    List<LocalDateTime> selectRegDtList(@Param("certTrgtEmlHmac") String certTrgtEmlHmac,
            @Param("emlCertPrpsCd") String emlCertPrpsCd, @Param("since") LocalDateTime since);

    /**
     * 인증 등록 (emlCertSn 채움)
     */
    int insert(EmailCertification cert);

    /**
     * 같은 이메일·목적의 쓰지 않은 이전 인증 무효화
     */
    int updateInvalidatePrevious(@Param("certTrgtEmlHmac") String certTrgtEmlHmac,
            @Param("emlCertPrpsCd") String emlCertPrpsCd, @Param("now") LocalDateTime now);

    /**
     * 인증 무효화
     */
    int updateInvalidate(@Param("emlCertSn") Long emlCertSn, @Param("now") LocalDateTime now);

    /**
     * 인증번호 실패 횟수 +1
     */
    int updateFailCount(Long emlCertSn);

    /**
     * 인증 완료 시각 기록 (처음 한 번만)
     */
    int updateCompleted(@Param("emlCertSn") Long emlCertSn, @Param("now") LocalDateTime now);

    /**
     * 인증 사용 완료 기록 (완료된 인증을 처음 한 번만, 영향 행이 0이면 이미 사용)
     */
    int updateUsed(@Param("emlCertSn") Long emlCertSn, @Param("now") LocalDateTime now);
}
