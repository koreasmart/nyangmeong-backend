package com.nyangmeong.community.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportReason;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;

/**
 * [게시글·댓글 신고 서비스]
 * - SCR-CMU-001 신고 접수, 실종센터·펫돌봄 상세도 같이 사용
 * - 누적 신고가 기준(SYS_STNG.BLIND_DCLR_ACML_CNT)에 닿으면 글은 블라인드, 댓글은 삭제 처리
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final SystemSettingService systemSettingService;

    /**
     * 신고 접수
     */
    @Transactional
    public ReportResponse create(Long mbrSn, ReportCreateRequest req) {
        if (req.pstSn() == null == (req.cmntSn() == null) || !ReportReason.exists(req.dclrRsnCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (ReportReason.ETC.is(req.dclrRsnCd()) && (req.dclrDtlCn() == null || req.dclrDtlCn().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean isPost = req.pstSn() != null;
        if (isPost) {
            if (postMapper.selectPost(req.pstSn(), PostStatus.NORMAL.getCode()) == null) {
                throw new CustomException(ErrorCode.POST_NOT_FOUND);
            }
        } else if (commentMapper.selectComment(req.cmntSn()) == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        ErrorCode duplicate = isPost ? ErrorCode.REPORT_ALREADY_EXISTS : ErrorCode.REPORT_COMMENT_ALREADY_EXISTS;
        if (reportMapper.existsReport(mbrSn, req.pstSn(), req.cmntSn())) {
            throw new CustomException(duplicate);
        }

        Report report = new Report();
        report.setMbrSn(mbrSn);
        report.setPstSn(req.pstSn());
        report.setCmntSn(req.cmntSn());
        report.setDclrRsnCd(req.dclrRsnCd());
        report.setDclrPrcsSttsCd(ReportStatus.RECEIVED.getCode());
        report.setDclrDtlCn(req.dclrDtlCn());
        try {
            reportMapper.insertReport(report);
        } catch (DuplicateKeyException e) {
            /** 동시에 같은 대상을 신고한 경우 */
            throw new CustomException(duplicate);
        }
        ReportStatusHistory history = new ReportStatusHistory();
        history.setDclrSn(report.getDclrSn());
        history.setMbrSn(mbrSn);
        history.setAftrSttsCd(ReportStatus.RECEIVED.getCode());
        reportMapper.insertStatusHistory(history);

        applyAccumulation(isPost, req);
        return ReportResponse.from(report);
    }

    /**
     * 누적 신고 기준 도달 시 자동 처리
     */
    private void applyAccumulation(boolean isPost, ReportCreateRequest req) {
        long limit = systemSettingService.get().getBlindDclrAcmlCnt();
        if (reportMapper.countReports(req.pstSn(), req.cmntSn()) < limit) {
            return;
        }
        if (!isPost) {
            commentMapper.updateDeleted(req.cmntSn());
            return;
        }
        if (postMapper.updateStatus(req.pstSn(), PostStatus.NORMAL.getCode(), PostStatus.BLIND.getCode(),
                "SYSTEM") > 0) {
            PostStatusHistory history = new PostStatusHistory();
            history.setPstSn(req.pstSn());
            history.setBfrSttsCd(PostStatus.NORMAL.getCode());
            history.setAftrSttsCd(PostStatus.BLIND.getCode());
            history.setChgRsn("신고 누적");
            postMapper.insertStatusHistory(history);
        }
    }
}
