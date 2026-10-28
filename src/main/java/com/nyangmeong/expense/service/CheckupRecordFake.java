package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.nyangmeong.expense.dto.CheckupCreateCommand;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.dto.ReceiptSaveResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * [검진기록·일정 생성 가짜]
 * - health 검진기록·일정 등록(JAJ) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 일정은 항상 없음, 검진기록은 번호만 돌려주고 저장하지 않는다
 * - 같은 요청을 다시 보냈을 때(멱등)의 응답을 위해 영수증별 결과만 메모리에 둔다
 */
@Slf4j
@Component
public class CheckupRecordFake {

    private final AtomicLong sequence = new AtomicLong(1000);
    private final Map<Long, List<ReceiptSaveResponse.CheckupRecord>> savedByReceipt = new ConcurrentHashMap<>();

    /**
     * 같은 날 병원 일정 존재 여부
     */
    public boolean existsHospitalSchedule(Long petSn, LocalDate ymd) {
        return false;
    }

    /**
     * 검진일 앞뒤 N일의 검진기록 미연결 병원 일정 조회
     */
    public List<ReceiptSaveCheckResponse.NearbySchedule> findNearbyUnlinkedSchedules(Long petSn, LocalDate ymd,
            int days) {
        return List.of();
    }

    /**
     * 검진기록 생성 (일정 연결·처방약 포함)
     */
    public Long create(CheckupCreateCommand command) {
        Long ckupRcdSn = sequence.incrementAndGet();
        log.info("[CheckupRecordFake] 검진기록 생성 recptSn={} petSn={} ckupRcdSn={}", command.recptSn(),
                command.petSn(), ckupRcdSn);
        return ckupRcdSn;
    }

    /**
     * 영수증 저장 결과 기억
     */
    public void remember(Long recptSn, List<ReceiptSaveResponse.CheckupRecord> records) {
        savedByReceipt.put(recptSn, records);
    }

    /**
     * 영수증으로 만든 검진기록 조회
     */
    public List<ReceiptSaveResponse.CheckupRecord> findByReceipt(Long recptSn) {
        return savedByReceipt.getOrDefault(recptSn, List.of());
    }
}
