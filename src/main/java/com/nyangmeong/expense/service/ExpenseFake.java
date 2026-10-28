package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.expense.dto.ExpenseCreateCommand;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * [가계부 지출 가짜]
 * - 지출 생성·중복 조회(SJM) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 중복은 항상 없음, 생성은 저장하지 않고 로그만 남긴다. 월 한도 알림은 지출 생성 쪽 몫이다
 */
@Slf4j
@Component
public class ExpenseFake {

    /**
     * 같은 반려동물·날짜·금액·카테고리 병원 지출 조회
     */
    public List<ReceiptSaveCheckResponse.DuplicateExpense> findDuplicates(Long mbrSn, Long petSn, LocalDate ymd,
            Long amt) {
        return List.of();
    }

    /**
     * 가계부 지출 생성
     */
    public void create(ExpenseCreateCommand command) {
        log.info("[ExpenseFake] 지출 생성 recptSn={} petSn={}", command.recptSn(), command.petSn());
    }
}
