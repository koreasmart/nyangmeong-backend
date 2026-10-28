package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptSource;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;

/**
 * [검사 - 영수증 업로드·다시 분석·분석 종료·취소]
 */
class ReceiptServiceTest {

    private final ReceiptMapper mapper = mock(ReceiptMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final ReceiptService service = new ReceiptService(mapper, fileStore, petAuthorizer);

    private final MockMultipartFile image = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    private Receipt receipt(ReceiptStatus status) {
        Receipt receipt = new Receipt();
        receipt.setRecptSn(3L);
        receipt.setMbrSn(1L);
        receipt.setAtchFileSn(7L);
        receipt.setOcrPrcsSttsCd(status.getCode());
        return receipt;
    }

    private ReceiptStatusHistory capturedHistory() {
        ArgumentCaptor<ReceiptStatusHistory> captor = ArgumentCaptor.forClass(ReceiptStatusHistory.class);
        verify(mapper).insertStatusHistory(captor.capture());
        return captor.getValue();
    }

    @Test
    void 업로드는_원본을_저장하고_건강기록_출처_분석중으로_등록한다() {
        when(fileStore.store(image, "1")).thenReturn(7L);
        doAnswer(invocation -> {
            invocation.<Receipt>getArgument(0).setRecptSn(3L);
            return null;
        }).when(mapper).insert(any());

        Long recptSn = service.create(1L, 10L, image);

        ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
        verify(mapper).insert(captor.capture());
        Receipt saved = captor.getValue();
        assertThat(recptSn).isEqualTo(3L);
        assertThat(saved.getMbrSn()).isEqualTo(1L);
        assertThat(saved.getAtchFileSn()).isEqualTo(7L);
        assertThat(saved.getOcrPrcsSttsCd()).isEqualTo(ReceiptStatus.ANALYZING.getCode());
        assertThat(saved.getOcrDmndSrcCd()).isEqualTo(ReceiptSource.HEALTH.getCode());
        ReceiptStatusHistory history = capturedHistory();
        assertThat(history.getRecptSn()).isEqualTo(3L);
        assertThat(history.getBfrSttsCd()).isNull();
        assertThat(history.getAftrSttsCd()).isEqualTo(ReceiptStatus.ANALYZING.getCode());
    }

    @Test
    void 연동되지_않은_반려동물이면_파일도_저장하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(1L, 99L);

        assertThatThrownBy(() -> service.create(1L, 99L, image))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_ACCESS_DENIED);
        verify(fileStore, never()).store(any(MockMultipartFile.class), anyString());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 취소_인식실패_일시오류는_다시_분석할_수_있다() {
        for (ReceiptStatus status : ReceiptStatus.RETRYABLE) {
            ReceiptMapper retryMapper = mock(ReceiptMapper.class);
            ReceiptService retryService = new ReceiptService(retryMapper, fileStore, petAuthorizer);
            when(retryMapper.selectOwned(3L, 1L)).thenReturn(receipt(status));
            when(retryMapper.updateStatus(eq(3L), anyList(), eq(ReceiptStatus.ANALYZING.getCode()))).thenReturn(1);

            retryService.restart(1L, 3L, 10L);

            ArgumentCaptor<ReceiptStatusHistory> captor = ArgumentCaptor.forClass(ReceiptStatusHistory.class);
            verify(retryMapper).insertStatusHistory(captor.capture());
            assertThat(captor.getValue().getBfrSttsCd()).isEqualTo(status.getCode());
            assertThat(captor.getValue().getAftrSttsCd()).isEqualTo(ReceiptStatus.ANALYZING.getCode());
        }
    }

    @Test
    void 분석중_영수증을_다시_분석하면_RECEIPT_ANALYZING() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.ANALYZING));

        assertThatThrownBy(() -> service.restart(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ANALYZING);
        verify(mapper, never()).updateStatus(anyLong(), anyList(), anyString());
    }

    @Test
    void 동시에_다시_분석하면_나중_요청은_RECEIPT_ANALYZING() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.CANCELED));
        when(mapper.updateStatus(eq(3L), anyList(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.restart(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ANALYZING);
        verify(mapper, never()).insertStatusHistory(any());
    }

    @Test
    void 저장완료_인식성공_영수증은_다시_분석하지_않는다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.SAVED));
        assertThatThrownBy(() -> service.restart(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ALREADY_SAVED);

        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.RECOGNIZED));
        assertThatThrownBy(() -> service.restart(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 남의_영수증은_RECEIPT_NOT_FOUND() {
        assertThatThrownBy(() -> service.restart(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
        assertThatThrownBy(() -> service.cancel(1L, 3L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
        assertThatThrownBy(() -> service.getImage(1L, 3L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }

    @Test
    void 분석_종료는_분석중일_때만_상태와_이력을_남긴다() {
        when(mapper.updateAnalyzed(3L, ReceiptStatus.ANALYZING.getCode(), ReceiptStatus.TEMPORARY_ERROR.getCode(),
                null)).thenReturn(1);

        boolean finished = service.finish(1L, 3L, ReceiptStatus.TEMPORARY_ERROR, "TIMEOUT", null);

        assertThat(finished).isTrue();
        ReceiptStatusHistory history = capturedHistory();
        assertThat(history.getBfrSttsCd()).isEqualTo(ReceiptStatus.ANALYZING.getCode());
        assertThat(history.getAftrSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(history.getChgRsn()).isEqualTo("TIMEOUT");
    }

    @Test
    void 분석_중_취소된_영수증은_분석_결과로_덮지_않는다() {
        when(mapper.updateAnalyzed(anyLong(), anyString(), anyString(), any())).thenReturn(0);

        assertThat(service.finish(1L, 3L, ReceiptStatus.RECOGNIZED, null, "{}")).isFalse();
        verify(mapper, never()).insertStatusHistory(any());
    }

    @Test
    void 분석중_영수증을_취소한다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.ANALYZING));
        when(mapper.updateStatus(3L, List.of(ReceiptStatus.ANALYZING.getCode()), ReceiptStatus.CANCELED.getCode()))
                .thenReturn(1);

        service.cancel(1L, 3L);

        ReceiptStatusHistory history = capturedHistory();
        assertThat(history.getBfrSttsCd()).isEqualTo(ReceiptStatus.ANALYZING.getCode());
        assertThat(history.getAftrSttsCd()).isEqualTo(ReceiptStatus.CANCELED.getCode());
    }

    @Test
    void 이미_취소했거나_분석이_끝난_영수증은_취소하지_않는다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.CANCELED));
        assertThatThrownBy(() -> service.cancel(1L, 3L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ALREADY_CANCELED);

        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.PARTIAL));
        assertThatThrownBy(() -> service.cancel(1L, 3L))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);

        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.ANALYZING));
        when(mapper.updateStatus(anyLong(), anyList(), anyString())).thenReturn(0);
        assertThatThrownBy(() -> service.cancel(1L, 3L))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
        verify(mapper, never()).insertStatusHistory(any());
    }
}
