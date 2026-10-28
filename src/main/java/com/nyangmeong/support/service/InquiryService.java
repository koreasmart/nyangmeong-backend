package com.nyangmeong.support.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.service.PixelCharacterService;
import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatus;
import com.nyangmeong.support.domain.InquiryStatusHistory;
import com.nyangmeong.support.domain.InquiryType;
import com.nyangmeong.support.dto.InquiryCreateRequest;
import com.nyangmeong.support.dto.InquiryImageSummary;
import com.nyangmeong.support.dto.InquiryResponse;
import com.nyangmeong.support.dto.InquirySearchCond;
import com.nyangmeong.support.dto.InquirySummary;
import com.nyangmeong.support.dto.InquiryUpdateRequest;
import com.nyangmeong.support.mapper.InquiryMapper;

import lombok.RequiredArgsConstructor;

/**
 * [1:1 문의 서비스]
 * - SCR-CSC-003 목록·작성·상세·수정, 관리자 답변 등록은 answer() 로 연결
 * - 캐릭터 오류 문의 : 저장 뒤 pet 의 receiveErrorInquiry 호출, 실패하면 문의도 롤백, 돌려받은 캐릭터 이미지를 첨부
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

    static final int MAX_IMAGES = 10;
    static final String MVMN_PATH = "/inquiries/";

    private final InquiryMapper inquiryMapper;
    private final FileStore fileStore;
    private final PixelCharacterService pixelCharacterService;
    private final EventPublisher eventPublisher;

    /**
     * 내 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<InquirySummary> search(Long mbrSn, InquirySearchCond cond, PageQuery pq) {
        String inqryPrcsSttsCd = cond.getInqryPrcsSttsCd();
        if (inqryPrcsSttsCd != null && inqryPrcsSttsCd.isBlank()) {
            inqryPrcsSttsCd = null;
        }
        if (inqryPrcsSttsCd != null && !InquiryStatus.exists(inqryPrcsSttsCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));
        List<InquirySummary> rows = inquiryMapper.selectList(mbrSn, inqryPrcsSttsCd);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 내 문의 상세 조회
     */
    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(Long mbrSn, Long otoInqrySn) {
        return toResponse(getOwnedOrThrow(mbrSn, otoInqrySn));
    }

    /**
     * 문의 작성
     */
    @Transactional
    public InquiryResponse create(Long mbrSn, InquiryCreateRequest req) {
        checkType(req.inqryTypeCd());
        boolean characterError = req.pixelCharSn() != null;
        if (characterError && !InquiryType.PIXEL_CHARACTER.is(req.inqryTypeCd())
                || req.charPhotoSn() != null && !characterError) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<MultipartFile> images = attached(req.images());
        if (images.size() + (characterError ? 1 : 0) > MAX_IMAGES) {
            throw new CustomException(ErrorCode.INQUIRY_ATTACHMENT_LIMIT_EXCEEDED);
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setMbrSn(mbrSn);
        inquiry.setPixelCharSn(req.pixelCharSn());
        inquiry.setCharPhotoSn(req.charPhotoSn());
        inquiry.setInqryTypeCd(req.inqryTypeCd());
        inquiry.setInqryPrcsSttsCd(InquiryStatus.RECEIVED.getCode());
        inquiry.setInqryTtl(req.inqryTtl());
        inquiry.setInqryCn(req.inqryCn());
        inquiryMapper.insertInquiry(inquiry);
        insertHistory(inquiry.getOtoInqrySn(), mbrSn, null, InquiryStatus.RECEIVED, null);

        int sortSeq = 0;
        if (characterError) {
            /** 캐릭터 쪽 접수 실패 시 문의도 함께 롤백, 돌려받은 캐릭터 이미지는 첫 첨부 */
            Long atchFileSn = pixelCharacterService.receiveErrorInquiry(mbrSn, req.pixelCharSn(),
                    inquiry.getOtoInqrySn());
            insertPhoto(inquiry.getOtoInqrySn(), atchFileSn, sortSeq++);
        }
        for (MultipartFile image : images) {
            insertPhoto(inquiry.getOtoInqrySn(), fileStore.store(image, String.valueOf(mbrSn)), sortSeq++);
        }
        return toResponse(inquiryMapper.selectInquiry(inquiry.getOtoInqrySn()));
    }

    /**
     * 문의 수정
     */
    @Transactional
    public InquiryResponse update(Long mbrSn, Long otoInqrySn, InquiryUpdateRequest req) {
        Inquiry inquiry = getOwnedOrThrow(mbrSn, otoInqrySn);
        if (!InquiryStatus.RECEIVED.is(inquiry.getInqryPrcsSttsCd())) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_EDITABLE);
        }
        if (req.inqryTypeCd() != null) {
            checkType(req.inqryTypeCd());
            /** 캐릭터 오류 문의는 유형을 바꿀 수 없음 */
            if (inquiry.getPixelCharSn() != null && !InquiryType.PIXEL_CHARACTER.is(req.inqryTypeCd())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        List<MultipartFile> images = attached(req.images());
        List<InquiryPhoto> remaining = new ArrayList<>(inquiryMapper.selectPhotos(otoInqrySn));
        List<Long> removed = new ArrayList<>();
        if (req.keepAtchFileSns() != null) {
            for (InquiryPhoto photo : remaining) {
                if (!req.keepAtchFileSns().contains(photo.getAtchFileSn())) {
                    removed.add(photo.getAtchFileSn());
                }
            }
            remaining.removeIf(photo -> removed.contains(photo.getAtchFileSn()));
        }
        if (remaining.size() + images.size() > MAX_IMAGES) {
            throw new CustomException(ErrorCode.INQUIRY_ATTACHMENT_LIMIT_EXCEEDED);
        }

        /** 접수 상태 조건을 UPDATE 에도 걸어 답변 등록과의 경합 차단 */
        if (inquiryMapper.updateInquiry(otoInqrySn, req.inqryTypeCd(), req.inqryTtl(), req.inqryCn(),
                InquiryStatus.RECEIVED.getCode()) == 0) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_EDITABLE);
        }
        if (!removed.isEmpty()) {
            inquiryMapper.updatePhotosDeleted(otoInqrySn, removed);
        }
        int sortSeq = remaining.stream().mapToInt(InquiryPhoto::getSortSeq).max().orElse(-1) + 1;
        for (MultipartFile image : images) {
            insertPhoto(otoInqrySn, fileStore.store(image, String.valueOf(mbrSn)), sortSeq++);
        }
        return toResponse(inquiryMapper.selectInquiry(otoInqrySn));
    }

    /**
     * 관리자 답변 등록
     */
    @Transactional
    public void answer(Long otoInqrySn, Long prcsMbrSn, String ansCn) {
        Inquiry inquiry = inquiryMapper.selectInquiry(otoInqrySn);
        if (inquiry == null) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_FOUND);
        }
        if (inquiryMapper.updateAnswer(otoInqrySn, prcsMbrSn, ansCn, InquiryStatus.RECEIVED.getCode(),
                InquiryStatus.ANSWERED.getCode()) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        insertHistory(otoInqrySn, prcsMbrSn, InquiryStatus.RECEIVED, InquiryStatus.ANSWERED, null);
        eventPublisher.publish(new NotificationEvent(inquiry.getMbrSn(), NotificationType.INQUIRY_ANSWER,
                "1:1 문의에 답변이 등록됐어요", inquiry.getInqryTtl(), MVMN_PATH + otoInqrySn));
    }

    /**
     * 본인 문의 조회
     */
    private Inquiry getOwnedOrThrow(Long mbrSn, Long otoInqrySn) {
        Inquiry inquiry = inquiryMapper.selectInquiry(otoInqrySn);
        if (inquiry == null) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_FOUND);
        }
        if (!mbrSn.equals(inquiry.getMbrSn())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return inquiry;
    }

    /**
     * 문의 유형 코드 검사
     */
    private void checkType(String inqryTypeCd) {
        if (!InquiryType.exists(inqryTypeCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 선택하지 않은 빈 파일 파트를 뺀 첨부 목록
     */
    private List<MultipartFile> attached(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    }

    private void insertPhoto(Long otoInqrySn, Long atchFileSn, int sortSeq) {
        InquiryPhoto photo = new InquiryPhoto();
        photo.setOtoInqrySn(otoInqrySn);
        photo.setAtchFileSn(atchFileSn);
        photo.setSortSeq(sortSeq);
        inquiryMapper.insertPhoto(photo);
    }

    private void insertHistory(Long otoInqrySn, Long mbrSn, InquiryStatus before, InquiryStatus after,
            String chgRsn) {
        InquiryStatusHistory history = new InquiryStatusHistory();
        history.setOtoInqrySn(otoInqrySn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(chgRsn);
        inquiryMapper.insertStatusHistory(history);
    }

    /**
     * 응답 변환 (유형·상태 이름과 첨부 포함)
     */
    private InquiryResponse toResponse(Inquiry inquiry) {
        List<InquiryImageSummary> imgs = inquiryMapper.selectPhotos(inquiry.getOtoInqrySn()).stream()
                .map(InquiryImageSummary::from).toList();
        return InquiryResponse.from(inquiry, inquiryMapper.selectCodeName(inquiry.getInqryTypeCd()),
                inquiryMapper.selectCodeName(inquiry.getInqryPrcsSttsCd()), imgs);
    }
}
