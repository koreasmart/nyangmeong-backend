package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.CheckupCreateCommand;
import com.nyangmeong.expense.dto.ExpenseCreateCommand;
import com.nyangmeong.expense.dto.ReceiptSaveCheckRequest;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.dto.ReceiptSaveDrugRequest;
import com.nyangmeong.expense.dto.ReceiptSavePetRequest;
import com.nyangmeong.expense.dto.ReceiptSaveRequest;
import com.nyangmeong.expense.dto.ReceiptSaveResponse;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 영수증 확인 후 저장·저장 전 확인]
 */
class ReceiptSaveServiceTest {

    private static final LocalDate YMD = LocalDate.of(2026, 10, 21);

    private final ReceiptMapper mapper = mock(ReceiptMapper.class);
    private final ReceiptService receiptService = mock(ReceiptService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CheckupRecordFake checkupFake = mock(CheckupRecordFake.class);
    private final ExpenseFake expenseFake = mock(ExpenseFake.class);
    private final ReceiptSaveService service = new ReceiptSaveService(mapper, receiptService, petAuthorizer,
            checkupFake, expenseFake);

    private Receipt receipt(ReceiptStatus status, String idmptDmndId) {
        Receipt receipt = new Receipt();
        receipt.setRecptSn(3L);
        receipt.setMbrSn(1L);
        receipt.setOcrPrcsSttsCd(status.getCode());
        receipt.setIdmptDmndId(idmptDmndId);
        return receipt;
    }

    private ReceiptSavePetRequest pet(Long petSn, Long amt, Boolean expenseYn, List<ReceiptSaveDrugRequest> drugs) {
        return new ReceiptSavePetRequest(petSn, "HLTC0002", "진찰료", null, amt, expenseYn, null, drugs);
    }

    private ReceiptSaveRequest request(ReceiptSavePetRequest... pets) {
        return requestWithPlace("냥멍동물병원", pets);
    }

    private ReceiptSaveRequest requestWithPlace(String expndPlcusNm, ReceiptSavePetRequest... pets) {
        return new ReceiptSaveRequest("req-1", "냥멍동물병원", YMD, expndPlcusNm, List.of(pets));
    }

    private void owned(ReceiptStatus status) {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(status, null));
        when(checkupFake.create(any())).thenReturn(501L, 502L);
    }

    @Test
    void 저장은_탭마다_검진기록을_만들고_상태를_저장완료로_바꾼다() {
        owned(ReceiptStatus.RECOGNIZED);

        ReceiptSaveService.Saved saved = service.save(1L, 3L,
                request(pet(10L, 30_000L, false, null), pet(11L, 20_000L, false, null)));

        assertThat(saved.created()).isTrue();
        assertThat(saved.response().ckupRcds()).containsExactly(
                new ReceiptSaveResponse.CheckupRecord(10L, 501L), new ReceiptSaveResponse.CheckupRecord(11L, 502L));
        verify(checkupFake, times(2)).create(any());
        verify(receiptService).markSaved(1L, 3L, ReceiptStatus.RECOGNIZED, "req-1");
        verify(checkupFake).remember(eq(3L), any());
    }

    @Test
    void 부분인식도_저장할_수_있다() {
        owned(ReceiptStatus.PARTIAL);

        assertThat(service.save(1L, 3L, request(pet(10L, null, false, null))).created()).isTrue();
        verify(receiptService).markSaved(1L, 3L, ReceiptStatus.PARTIAL, "req-1");
    }

    @Test
    void 처방약은_케어리스트에_반영하지_않는_값으로_넘긴다() {
        owned(ReceiptStatus.RECOGNIZED);
        ReceiptSaveDrugRequest drug = new ReceiptSaveDrugRequest("항생제", YMD, YMD.plusDays(5), 2, "HLTC0010");

        service.save(1L, 3L, request(pet(10L, 30_000L, false, List.of(drug))));

        ArgumentCaptor<CheckupCreateCommand> captor = ArgumentCaptor.forClass(CheckupCreateCommand.class);
        verify(checkupFake).create(captor.capture());
        assertThat(captor.getValue().careRfltYn()).isEqualTo("N");
        assertThat(captor.getValue().drugs()).containsExactly(drug);
        assertThat(captor.getValue().hsptlNm()).isEqualTo("냥멍동물병원");
    }

    @Test
    void 가계부는_체크하고_금액이_1원_이상인_탭만_기록한다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.RECOGNIZED, null));
        when(checkupFake.create(any())).thenReturn(501L, 502L, 503L);

        service.save(1L, 3L, request(pet(10L, 30_000L, true, null), pet(11L, 0L, true, null),
                pet(12L, 5_000L, false, null)));

        ArgumentCaptor<ExpenseCreateCommand> captor = ArgumentCaptor.forClass(ExpenseCreateCommand.class);
        verify(expenseFake, times(1)).create(captor.capture());
        assertThat(captor.getValue().petSn()).isEqualTo(10L);
        assertThat(captor.getValue().expndAmt()).isEqualTo(30_000L);
        assertThat(captor.getValue().expndYmd()).isEqualTo(YMD);
        assertThat(captor.getValue().expndPlcusNm()).isEqualTo("냥멍동물병원");
        assertThat(captor.getValue().dtlMttr()).isNull();
        assertThat(captor.getValue().recptSn()).isEqualTo(3L);
    }

    @Test
    void 가계부_사용처가_비면_사용처_없이_기록한다() {
        owned(ReceiptStatus.RECOGNIZED);

        service.save(1L, 3L, requestWithPlace("  ", pet(10L, 30_000L, true, null)));

        ArgumentCaptor<ExpenseCreateCommand> captor = ArgumentCaptor.forClass(ExpenseCreateCommand.class);
        verify(expenseFake).create(captor.capture());
        assertThat(captor.getValue().expndPlcusNm()).isNull();
        assertThat(captor.getValue().dtlMttr()).isNull();
    }

    @Test
    void 가계부_사용처는_병원명과_달라도_입력값을_쓴다() {
        owned(ReceiptStatus.RECOGNIZED);

        service.save(1L, 3L, requestWithPlace("행복동물병원 본점", pet(10L, 30_000L, true, null)));

        ArgumentCaptor<ExpenseCreateCommand> captor = ArgumentCaptor.forClass(ExpenseCreateCommand.class);
        verify(expenseFake).create(captor.capture());
        assertThat(captor.getValue().expndPlcusNm()).isEqualTo("행복동물병원 본점");
    }

    @Test
    void 지출_생성이_실패하면_상태_변경_뒤에_예외가_번져_롤백된다() {
        owned(ReceiptStatus.RECOGNIZED);
        doThrow(new IllegalStateException("지출 실패")).when(expenseFake).create(any());

        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, 30_000L, true, null))))
                .isInstanceOf(IllegalStateException.class);
        verify(checkupFake, never()).remember(anyLong(), any());
    }

    @Test
    void 같은_멱등_ID로_다시_보내면_새로_만들지_않고_같은_결과를_돌려준다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.SAVED, "req-1"));
        List<ReceiptSaveResponse.CheckupRecord> saved = List.of(new ReceiptSaveResponse.CheckupRecord(10L, 501L));
        when(checkupFake.findByReceipt(3L)).thenReturn(saved);

        ReceiptSaveService.Saved result = service.save(1L, 3L, request(pet(10L, 30_000L, true, null)));

        assertThat(result.created()).isFalse();
        assertThat(result.response().ckupRcds()).isEqualTo(saved);
        verify(checkupFake, never()).create(any());
        verify(expenseFake, never()).create(any());
        verify(receiptService, never()).markSaved(anyLong(), anyLong(), any(), any());
    }

    @Test
    void 다른_멱등_ID로_저장된_영수증은_이미_저장_409() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.SAVED, "other"));

        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, null, false, null))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RECEIPT_ALREADY_SAVED));
    }

    @Test
    void 다른_영수증이_쓴_멱등_ID는_이미_처리된_요청_409() {
        owned(ReceiptStatus.RECOGNIZED);
        Receipt other = receipt(ReceiptStatus.SAVED, "req-1");
        other.setRecptSn(99L);
        when(mapper.selectByIdmptDmndId("req-1")).thenReturn(other);

        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, null, false, null))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED));
        verify(checkupFake, never()).create(any());
    }

    @Test
    void 저장할_수_없는_상태는_상태별_409로_막는다() {
        assertConflict(ReceiptStatus.ANALYZING, ErrorCode.RECEIPT_ANALYZING);
        assertConflict(ReceiptStatus.CANCELED, ErrorCode.RECEIPT_ALREADY_CANCELED);
        assertConflict(ReceiptStatus.NOT_RECOGNIZED, ErrorCode.RECEIPT_NOT_SAVABLE);
        assertConflict(ReceiptStatus.TEMPORARY_ERROR, ErrorCode.RECEIPT_NOT_SAVABLE);
    }

    private void assertConflict(ReceiptStatus status, ErrorCode expected) {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(status, null));
        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, null, false, null))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 남의_영수증은_찾을_수_없다() {
        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, null, false, null))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RECEIPT_NOT_FOUND));
    }

    @Test
    void 같은_반려동물이_두_탭이면_입력값_오류다() {
        owned(ReceiptStatus.RECOGNIZED);

        assertThatThrownBy(() -> service.save(1L, 3L,
                request(pet(10L, null, false, null), pet(10L, null, false, null))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(receiptService, never()).markSaved(anyLong(), anyLong(), any(), any());
    }

    @Test
    void 연동되지_않은_반려동물이_있으면_하나도_저장하지_않는다() {
        owned(ReceiptStatus.RECOGNIZED);
        doThrow(new CustomException(ErrorCode.FORBIDDEN)).when(petAuthorizer).checkCaregiver(1L, 11L);

        assertThatThrownBy(() -> service.save(1L, 3L,
                request(pet(10L, null, false, null), pet(11L, null, false, null))))
                .isInstanceOf(CustomException.class);
        verify(checkupFake, never()).create(any());
        verify(receiptService, never()).markSaved(anyLong(), anyLong(), any(), any());
    }

    @Test
    void 복용_종료일이_시작일보다_앞이면_입력값_오류다() {
        owned(ReceiptStatus.RECOGNIZED);
        ReceiptSaveDrugRequest drug = new ReceiptSaveDrugRequest("항생제", YMD, YMD.minusDays(1), 2, "HLTC0010");

        assertThatThrownBy(() -> service.save(1L, 3L, request(pet(10L, null, false, List.of(drug)))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 금액_합계가_달라도_저장은_막지_않는다() {
        owned(ReceiptStatus.RECOGNIZED);

        assertThat(service.save(1L, 3L, request(pet(10L, 1L, false, null))).created()).isTrue();
    }

    @Test
    void 저장_전_확인은_체크한_탭의_중복_지출과_가까운_일정을_돌려준다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.RECOGNIZED, null));
        ReceiptSaveCheckResponse.DuplicateExpense duplicate = new ReceiptSaveCheckResponse.DuplicateExpense(10L, 7L,
                YMD, 30_000L, null, "냥멍동물병원");
        ReceiptSaveCheckResponse.NearbySchedule nearby = new ReceiptSaveCheckResponse.NearbySchedule(10L, 8L,
                LocalDateTime.of(2026, 10, 23, 10, 0), "냥멍동물병원");
        when(expenseFake.findDuplicates(1L, 10L, YMD, 30_000L)).thenReturn(List.of(duplicate));
        when(checkupFake.existsHospitalSchedule(10L, YMD)).thenReturn(false);
        when(checkupFake.findNearbyUnlinkedSchedules(eq(10L), eq(YMD), anyInt())).thenReturn(List.of(nearby));

        ReceiptSaveCheckResponse result = service.check(1L, 3L, new ReceiptSaveCheckRequest(YMD, List.of(
                new ReceiptSaveCheckRequest.Pet(10L, 30_000L, true),
                new ReceiptSaveCheckRequest.Pet(11L, 5_000L, false))));

        assertThat(result.duplicateExpenses()).containsExactly(duplicate);
        assertThat(result.nearbySchedules()).contains(nearby);
        verify(expenseFake, never()).findDuplicates(eq(1L), eq(11L), any(), any());
        verify(checkupFake, never()).create(any());
    }

    @Test
    void 같은_날_병원_일정이_있으면_가까운_일정을_찾지_않는다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(receipt(ReceiptStatus.RECOGNIZED, null));
        when(checkupFake.existsHospitalSchedule(10L, YMD)).thenReturn(true);

        ReceiptSaveCheckResponse result = service.check(1L, 3L, new ReceiptSaveCheckRequest(YMD,
                List.of(new ReceiptSaveCheckRequest.Pet(10L, 30_000L, false))));

        assertThat(result.nearbySchedules()).isEmpty();
        verify(checkupFake, never()).findNearbyUnlinkedSchedules(anyLong(), any(), anyInt());
    }
}
