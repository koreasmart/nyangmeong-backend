package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.pet.service.PixelCharacterService;
import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatus;
import com.nyangmeong.support.domain.InquiryStatusHistory;
import com.nyangmeong.support.domain.InquiryType;
import com.nyangmeong.support.dto.InquiryCreateRequest;
import com.nyangmeong.support.dto.InquiryResponse;
import com.nyangmeong.support.dto.InquirySearchCond;
import com.nyangmeong.support.dto.InquiryUpdateRequest;
import com.nyangmeong.support.mapper.InquiryMapper;

/**
 * [검사 - 1:1 문의 목록·작성·상세·수정·답변]
 */
class InquiryServiceTest {

    private static final Long MBR = 1L;
    private static final Long INQ = 7L;

    private final InquiryMapper inquiryMapper = mock(InquiryMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final PixelCharacterService pixelCharacterService = mock(PixelCharacterService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final InquiryService service = new InquiryService(inquiryMapper, fileStore, pixelCharacterService,
            eventPublisher);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Inquiry inquiry(Long mbrSn, InquiryStatus status) {
        Inquiry inquiry = new Inquiry();
        inquiry.setOtoInqrySn(INQ);
        inquiry.setMbrSn(mbrSn);
        inquiry.setInqryTypeCd(InquiryType.COMMUNITY.getCode());
        inquiry.setInqryPrcsSttsCd(status.getCode());
        inquiry.setInqryTtl("제목");
        inquiry.setInqryCn("내용");
        inquiry.setRegDt(LocalDateTime.of(2026, 10, 26, 9, 0));
        return inquiry;
    }

    private InquiryPhoto photo(Long atchFileSn, int sortSeq) {
        InquiryPhoto photo = new InquiryPhoto();
        photo.setOtoInqrySn(INQ);
        photo.setAtchFileSn(atchFileSn);
        photo.setSortSeq(sortSeq);
        return photo;
    }

    private MultipartFile image(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[] {1});
    }

    private InquiryCreateRequest createRequest(String type, List<MultipartFile> images, Long pixelCharSn) {
        return new InquiryCreateRequest(type, "제목", "내용", images, pixelCharSn, pixelCharSn == null ? null : 5L);
    }

    /** 저장된 문의를 조회 결과로 돌려주도록 준비 */
    private void stubSavedInquiry(Inquiry saved) {
        doAnswer(invocation -> {
            Inquiry arg = invocation.getArgument(0);
            arg.setOtoInqrySn(INQ);
            saved.setMbrSn(arg.getMbrSn());
            saved.setInqryTypeCd(arg.getInqryTypeCd());
            saved.setInqryPrcsSttsCd(arg.getInqryPrcsSttsCd());
            return 1;
        }).when(inquiryMapper).insertInquiry(any(Inquiry.class));
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(saved);
    }

    @Test
    void 목록은_처리_상태_필터를_그대로_넘기고_빈_값은_전체로_본다() {
        InquirySearchCond cond = new InquirySearchCond();
        cond.setInqryPrcsSttsCd(InquiryStatus.ANSWERED.getCode());
        service.search(MBR, cond, new PageQuery(null, null, null));
        verify(inquiryMapper).selectList(MBR, "CSCC0013");

        cond.setInqryPrcsSttsCd(" ");
        service.search(MBR, cond, new PageQuery(null, null, null));
        verify(inquiryMapper).selectList(MBR, null);
    }

    @Test
    void 알_수_없는_처리_상태는_400() {
        InquirySearchCond cond = new InquirySearchCond();
        cond.setInqryPrcsSttsCd("CSCC9999");

        assertThatThrownBy(() -> service.search(MBR, cond, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 작성하면_접수_상태로_저장하고_이력과_첨부를_남긴다() {
        stubSavedInquiry(inquiry(MBR, InquiryStatus.RECEIVED));
        when(fileStore.store(any(MultipartFile.class), anyString())).thenReturn(101L, 102L);
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(101L, 0), photo(102L, 1)));
        when(inquiryMapper.selectCodeName("CSCC0008")).thenReturn("커뮤니티");
        when(inquiryMapper.selectCodeName("CSCC0012")).thenReturn("접수");

        InquiryResponse res = service.create(MBR,
                createRequest(InquiryType.COMMUNITY.getCode(), List.of(image("a.png"), image("b.png")), null));

        ArgumentCaptor<Inquiry> saved = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryMapper).insertInquiry(saved.capture());
        assertThat(saved.getValue().getInqryPrcsSttsCd()).isEqualTo("CSCC0012");
        ArgumentCaptor<InquiryStatusHistory> history = ArgumentCaptor.forClass(InquiryStatusHistory.class);
        verify(inquiryMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CSCC0012");
        verify(inquiryMapper, org.mockito.Mockito.times(2)).insertPhoto(any(InquiryPhoto.class));
        verify(pixelCharacterService, never()).receiveErrorInquiry(anyLong(), anyLong(), anyLong());
        assertThat(res.inqryTypeCdNm()).isEqualTo("커뮤니티");
        assertThat(res.imgs()).hasSize(2);
    }

    @Test
    void 알_수_없는_문의_유형은_400() {
        assertThatThrownBy(() -> service.create(MBR, createRequest("CSCC9999", List.of(), null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(inquiryMapper, never()).insertInquiry(any(Inquiry.class));
    }

    @Test
    void 이미지는_최대_10개이고_11개면_400() {
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            images.add(image(i + ".png"));
        }

        assertThatThrownBy(() -> service.create(MBR, createRequest(InquiryType.ETC.getCode(), images, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_ATTACHMENT_LIMIT_EXCEEDED));
        verify(inquiryMapper, never()).insertInquiry(any(Inquiry.class));
    }

    @Test
    void 선택하지_않아_비어_있는_파일_파트는_첨부로_세지_않는다() {
        stubSavedInquiry(inquiry(MBR, InquiryStatus.RECEIVED));
        MultipartFile empty = new MockMultipartFile("images", "", "application/octet-stream", new byte[0]);

        service.create(MBR, createRequest(InquiryType.ETC.getCode(), List.of(empty), null));

        verify(fileStore, never()).store(any(MultipartFile.class), anyString());
        verify(inquiryMapper, never()).insertPhoto(any(InquiryPhoto.class));
    }

    @Test
    void 캐릭터_오류_문의는_캐릭터_번호를_저장하고_pet_접수가_돌려준_이미지를_첫_첨부로_붙인다() {
        stubSavedInquiry(inquiry(MBR, InquiryStatus.RECEIVED));
        when(pixelCharacterService.receiveErrorInquiry(MBR, 3L, INQ)).thenReturn(900L);
        when(fileStore.store(any(MultipartFile.class), anyString())).thenReturn(101L);

        service.create(MBR, createRequest(InquiryType.PIXEL_CHARACTER.getCode(), List.of(image("a.png")), 3L));

        ArgumentCaptor<Inquiry> saved = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryMapper).insertInquiry(saved.capture());
        assertThat(saved.getValue().getPixelCharSn()).isEqualTo(3L);
        assertThat(saved.getValue().getCharPhotoSn()).isEqualTo(5L);
        verify(pixelCharacterService).receiveErrorInquiry(MBR, 3L, INQ);
        ArgumentCaptor<InquiryPhoto> photos = ArgumentCaptor.forClass(InquiryPhoto.class);
        verify(inquiryMapper, org.mockito.Mockito.times(2)).insertPhoto(photos.capture());
        assertThat(photos.getAllValues().get(0).getAtchFileSn()).isEqualTo(900L);
        assertThat(photos.getAllValues().get(0).getSortSeq()).isZero();
        assertThat(photos.getAllValues().get(1).getAtchFileSn()).isEqualTo(101L);
        assertThat(photos.getAllValues().get(1).getSortSeq()).isEqualTo(1);
    }

    @Test
    void 캐릭터_이미지를_합쳐_첨부가_10개를_넘으면_400() {
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            images.add(image(i + ".png"));
        }

        assertThatThrownBy(() -> service.create(MBR,
                createRequest(InquiryType.PIXEL_CHARACTER.getCode(), images, 3L)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_ATTACHMENT_LIMIT_EXCEEDED));
        verify(inquiryMapper, never()).insertInquiry(any(Inquiry.class));
    }

    @Test
    void 캐릭터_사진_번호만_보내면_400() {
        InquiryCreateRequest req = new InquiryCreateRequest(InquiryType.PIXEL_CHARACTER.getCode(), "제목", "내용",
                List.of(), null, 5L);

        assertThatThrownBy(() -> service.create(MBR, req)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 캐릭터_번호는_픽셀_캐릭터_유형일_때만_받는다() {
        assertThatThrownBy(() -> service.create(MBR, createRequest(InquiryType.COMMUNITY.getCode(), List.of(), 3L)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(pixelCharacterService, never()).receiveErrorInquiry(anyLong(), anyLong(), anyLong());
    }

    @Test
    void 이미_접수된_캐릭터면_예외를_그대로_던져_문의_저장을_롤백한다() {
        stubSavedInquiry(inquiry(MBR, InquiryStatus.RECEIVED));
        doThrow(new CustomException(ErrorCode.PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED))
                .when(pixelCharacterService).receiveErrorInquiry(MBR, 3L, INQ);

        assertThatThrownBy(() -> service.create(MBR,
                createRequest(InquiryType.PIXEL_CHARACTER.getCode(), List.of(image("a.png")), 3L)))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorCode.PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED));
        verify(fileStore, never()).store(any(MultipartFile.class), anyString());
    }

    @Test
    void 상세는_답변_전이면_답변을_비워_돌려준다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(101L, 0)));

        InquiryResponse res = service.getInquiry(MBR, INQ);

        assertThat(res.ansCn()).isNull();
        assertThat(res.ansDt()).isNull();
        assertThat(res.imgs()).hasSize(1);
    }

    @Test
    void 남의_문의는_403_FORBIDDEN_없는_번호는_404() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(2L, InquiryStatus.RECEIVED));

        assertThatThrownBy(() -> service.getInquiry(MBR, INQ)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> service.getInquiry(MBR, 99L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));
        assertThatThrownBy(() -> service.update(MBR, INQ, new InquiryUpdateRequest(null, "제목", null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 답변_완료된_문의는_수정할_수_없다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.ANSWERED));

        assertThatThrownBy(() -> service.update(MBR, INQ, new InquiryUpdateRequest(null, "새 제목", null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_EDITABLE));
        verify(inquiryMapper, never()).updateInquiry(anyLong(), any(), any(), any(), any());
    }

    @Test
    void 수정은_남길_첨부만_두고_새_첨부를_뒤에_붙인다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(101L, 0), photo(102L, 1)));
        when(inquiryMapper.updateInquiry(eq(INQ), any(), eq("새 제목"), any(), eq("CSCC0012"))).thenReturn(1);
        when(fileStore.store(any(MultipartFile.class), anyString())).thenReturn(103L);

        service.update(MBR, INQ, new InquiryUpdateRequest(null, "새 제목", null, List.of(102L), List.of(image("c.png"))));

        verify(inquiryMapper).updatePhotosDeleted(INQ, List.of(101L));
        ArgumentCaptor<InquiryPhoto> added = ArgumentCaptor.forClass(InquiryPhoto.class);
        verify(inquiryMapper).insertPhoto(added.capture());
        assertThat(added.getValue().getAtchFileSn()).isEqualTo(103L);
        assertThat(added.getValue().getSortSeq()).isEqualTo(2);
    }

    @Test
    void 남길_첨부를_보내지_않으면_기존_첨부를_그대로_둔다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(101L, 0)));
        when(inquiryMapper.updateInquiry(eq(INQ), any(), any(), eq("새 내용"), any())).thenReturn(1);

        service.update(MBR, INQ, new InquiryUpdateRequest(null, null, "새 내용", null, null));

        verify(inquiryMapper, never()).updatePhotosDeleted(anyLong(), any());
    }

    @Test
    void 수정해도_첨부는_합쳐서_10개를_넘을_수_없다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        List<InquiryPhoto> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(photo(100L + i, i));
        }
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(nine);

        assertThatThrownBy(() -> service.update(MBR, INQ,
                new InquiryUpdateRequest(null, null, null, null, List.of(image("a.png"), image("b.png")))))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_ATTACHMENT_LIMIT_EXCEEDED));
        verify(inquiryMapper, never()).updateInquiry(anyLong(), any(), any(), any(), any());
    }

    @Test
    void 접수_상태_조건에_걸리지_않으면_수정은_NOT_EDITABLE() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of());
        when(inquiryMapper.updateInquiry(eq(INQ), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.update(MBR, INQ, new InquiryUpdateRequest(null, "제목", null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_EDITABLE));
    }

    @Test
    void 답변을_등록하면_상태_이력을_남기고_답변_알림을_발행한다() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.RECEIVED));
        when(inquiryMapper.updateAnswer(INQ, 5L, "답변", "CSCC0012", "CSCC0013")).thenReturn(1);

        service.answer(INQ, 5L, "답변");

        ArgumentCaptor<InquiryStatusHistory> history = ArgumentCaptor.forClass(InquiryStatusHistory.class);
        verify(inquiryMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo("CSCC0012");
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CSCC0013");
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(MBR);
        assertThat(event.getValue().type()).isEqualTo(NotificationType.INQUIRY_ANSWER);
        assertThat(event.getValue().mvmnPath()).isEqualTo("/inquiries/7");
    }

    @Test
    void 이미_답변된_문의에_다시_답변하면_ALREADY_PROCESSED() {
        when(inquiryMapper.selectInquiry(INQ)).thenReturn(inquiry(MBR, InquiryStatus.ANSWERED));
        when(inquiryMapper.updateAnswer(eq(INQ), anyLong(), anyString(), anyString(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.answer(INQ, 5L, "답변")).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED));
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }
}
