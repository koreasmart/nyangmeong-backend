package com.nyangmeong.expense.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.expense.dto.CaregiverPetSummary;

/**
 * [연동 반려동물 목록 가짜]
 * - pet 연동 반려동물 조회(SJM) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 빈 목록 : 이름 자동 연결 없음, 구역 없는 영수증만 고른 반려동물로 연결
 */
@Component
public class CaregiverPetFake {

    /**
     * 연동 반려동물 목록 조회
     */
    public List<CaregiverPetSummary> search(Long mbrSn) {
        return List.of();
    }
}
