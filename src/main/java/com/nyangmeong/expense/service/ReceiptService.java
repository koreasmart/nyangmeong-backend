package com.nyangmeong.expense.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptSource;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;

import lombok.RequiredArgsConstructor;

/**
 * [영수증 상태 처리]
 * - 업로드·다시 분석·분석 종료·취소 각각 별도 트랜잭션 : OCR 호출은 트랜잭션 밖 (ReceiptAnalyzer)
 * - 검진 경로 전용 : 출처 = 건강기록 (FN-AIM-008-02)
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptMapper receiptMapper;
    private final FileStore fileStore;
    private final PetAuthorizer petAuthorizer;

    /**
     * 영수증 업로드
     */
    @Transactional
    public Long create(Long mbrSn, Long petSn, MultipartFile file) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        Long atchFileSn = fileStore.store(file, String.valueOf(mbrSn));

        Receipt receipt = new Receipt();
        receipt.setMbrSn(mbrSn);
        receipt.setAtchFileSn(atchFileSn);
        receipt.setOcrPrcsSttsCd(ReceiptStatus.ANALYZING.getCode());
        receipt.setOcrDmndSrcCd(ReceiptSource.HEALTH.getCode());
        receiptMapper.insert(receipt);
        insertHistory(receipt.getRecptSn(), mbrSn, null, ReceiptStatus.ANALYZING, null);
        return receipt.getRecptSn();
    }

    /**
     * 다시 분석 시작
     */
    @Transactional
    public void restart(Long mbrSn, Long recptSn, Long petSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ReceiptStatus before = ReceiptStatus.from(getOwned(mbrSn, recptSn).getOcrPrcsSttsCd());
        if (!ReceiptStatus.RETRYABLE.contains(before)) {
            throw conflict(before);
        }
        if (receiptMapper.updateStatus(recptSn, codes(ReceiptStatus.RETRYABLE),
                ReceiptStatus.ANALYZING.getCode()) == 0) {
            throw new CustomException(ErrorCode.RECEIPT_ANALYZING);
        }
        insertHistory(recptSn, mbrSn, before, ReceiptStatus.ANALYZING, null);
    }

    /**
     * 영수증 원본 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long mbrSn, Long recptSn) {
        return fileStore.load(getOwned(mbrSn, recptSn).getAtchFileSn());
    }

    /**
     * 분석 종료
     */
    @Transactional
    public boolean finish(Long mbrSn, Long recptSn, ReceiptStatus after, String chgRsn, String ocrRspnsJson) {
        if (receiptMapper.updateAnalyzed(recptSn, ReceiptStatus.ANALYZING.getCode(), after.getCode(),
                ocrRspnsJson) == 0) {
            return false;
        }
        insertHistory(recptSn, mbrSn, ReceiptStatus.ANALYZING, after, chgRsn);
        return true;
    }

    /**
     * 분석 취소
     */
    @Transactional
    public void cancel(Long mbrSn, Long recptSn) {
        ReceiptStatus before = ReceiptStatus.from(getOwned(mbrSn, recptSn).getOcrPrcsSttsCd());
        if (before != ReceiptStatus.ANALYZING) {
            throw conflict(before);
        }
        if (receiptMapper.updateStatus(recptSn, codes(List.of(ReceiptStatus.ANALYZING)),
                ReceiptStatus.CANCELED.getCode()) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        insertHistory(recptSn, mbrSn, before, ReceiptStatus.CANCELED, null);
    }

    /**
     * 저장 완료 처리 : 인식성공·부분인식에서만
     */
    @Transactional
    public void markSaved(Long mbrSn, Long recptSn, ReceiptStatus before, String idmptDmndId) {
        if (receiptMapper.updateSaved(recptSn, codes(List.of(ReceiptStatus.RECOGNIZED, ReceiptStatus.PARTIAL)),
                ReceiptStatus.SAVED.getCode(), idmptDmndId) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        insertHistory(recptSn, mbrSn, before, ReceiptStatus.SAVED, null);
    }

    /**
     * 회원 영수증 조회
     */
    private Receipt getOwned(Long mbrSn, Long recptSn) {
        Receipt receipt = receiptMapper.selectOwned(recptSn, mbrSn);
        if (receipt == null) {
            throw new CustomException(ErrorCode.RECEIPT_NOT_FOUND);
        }
        return receipt;
    }

    /**
     * 현재 상태별 충돌 예외
     */
    private CustomException conflict(ReceiptStatus status) {
        return switch (status) {
            case ANALYZING -> new CustomException(ErrorCode.RECEIPT_ANALYZING);
            case CANCELED -> new CustomException(ErrorCode.RECEIPT_ALREADY_CANCELED);
            case SAVED -> new CustomException(ErrorCode.RECEIPT_ALREADY_SAVED);
            default -> new CustomException(ErrorCode.ALREADY_PROCESSED);
        };
    }

    /**
     * 상태 코드 목록
     */
    private static List<String> codes(List<ReceiptStatus> statuses) {
        return statuses.stream().map(ReceiptStatus::getCode).toList();
    }

    /**
     * 상태 이력 등록
     */
    private void insertHistory(Long recptSn, Long mbrSn, ReceiptStatus before, ReceiptStatus after, String chgRsn) {
        ReceiptStatusHistory history = new ReceiptStatusHistory();
        history.setRecptSn(recptSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(chgRsn);
        receiptMapper.insertStatusHistory(history);
    }
}
