package com.nyangmeong.scheduler;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * [연동 양육자 조회 가짜]
 * - pet 양육자 목록 조회(SJM, 기능 분배 3.1절 6번) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 빈 목록 : HospitalScheduleFake 와 같은 이유
 */
@Component
public class CaregiverFake {

    /**
     * 반려동물의 연동 양육자 회원 번호 목록 조회
     * - 연결 대상 조건 : 주양육자 + 연동 중 공동양육자 (BRUP_REL.USE_YN = 'Y', FN-AIM-006-20)
     */
    public List<Long> search(Long petSn) {
        return List.of();
    }
}
