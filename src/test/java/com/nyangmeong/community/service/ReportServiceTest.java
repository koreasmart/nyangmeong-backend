package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 게시글·댓글 신고 접수와 누적 처리]
 */
class ReportServiceTest {

    private static final Long MBR = 1L;
    private static final Long PST = 7L;
    private static final Long CMNT = 20L;
    private static final Long DCLR = 55L;
    private static final String NORMAL = "CMUC0003";

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final ReportService service = new ReportService(reportMapper, postMapper, commentMapper,
            systemSettingService);

    private void stubTargets() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(new Post());
        when(commentMapper.selectComment(CMNT)).thenReturn(new Comment());
        doAnswer(invocation -> {
            Report arg = invocation.getArgument(0);
            arg.setDclrSn(DCLR);
            return 1;
        }).when(reportMapper).insertReport(any(Report.class));
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 글_신고는_접수_상태로_저장하고_이력을_남긴다() {
        stubTargets();
        when(reportMapper.countReports(PST, null)).thenReturn(1);

        ReportResponse res = service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0009", null));

        assertThat(res.dclrSn()).isEqualTo(DCLR);
        assertThat(res.dclrPrcsSttsCd()).isEqualTo("CMUC0015");
        ArgumentCaptor<ReportStatusHistory> history = ArgumentCaptor.forClass(ReportStatusHistory.class);
        verify(reportMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CMUC0015");
        verify(postMapper, never()).updateStatus(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 글_신고가_5건째면_블라인드_처리하고_이력을_남긴다() {
        stubTargets();
        when(reportMapper.countReports(PST, null)).thenReturn(5);
        when(postMapper.updateStatus(PST, NORMAL, "CMUC0004", "SYSTEM")).thenReturn(1);

        service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0010", null));

        ArgumentCaptor<PostStatusHistory> history = ArgumentCaptor.forClass(PostStatusHistory.class);
        verify(postMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(NORMAL);
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CMUC0004");
    }

    @Test
    void 댓글_신고가_4건이면_그대로이고_5건째면_삭제_처리() {
        stubTargets();
        when(reportMapper.countReports(null, CMNT)).thenReturn(4, 5);

        service.create(MBR, new ReportCreateRequest(null, CMNT, "CMUC0009", null));
        verify(commentMapper, never()).updateDeleted(anyLong());

        service.create(MBR, new ReportCreateRequest(null, CMNT, "CMUC0009", null));
        verify(commentMapper).updateDeleted(CMNT);
    }

    @Test
    void 글과_댓글_중_하나만_지정해야_한다() {
        assertError(() -> service.create(MBR, new ReportCreateRequest(null, null, "CMUC0009", null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, CMNT, "CMUC0009", null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 알_수_없는_사유는_400이고_기타는_상세_내용이_필수() {
        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, null, "CMUC9999", null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0014", " ")),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void 기타도_상세_내용이_있으면_접수한다() {
        stubTargets();

        service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0014", "직접 입력"));

        verify(reportMapper).insertReport(any(Report.class));
    }

    @Test
    void 없는_글이나_댓글은_404() {
        when(commentMapper.selectComment(CMNT)).thenReturn(null);

        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0009", null)),
                ErrorCode.POST_NOT_FOUND);
        assertError(() -> service.create(MBR, new ReportCreateRequest(null, CMNT, "CMUC0009", null)),
                ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 같은_회원이_같은_대상을_또_신고하면_409() {
        stubTargets();
        when(reportMapper.existsReport(MBR, PST, null)).thenReturn(true);
        when(reportMapper.existsReport(MBR, null, CMNT)).thenReturn(true);

        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0009", null)),
                ErrorCode.REPORT_ALREADY_EXISTS);
        assertError(() -> service.create(MBR, new ReportCreateRequest(null, CMNT, "CMUC0009", null)),
                ErrorCode.REPORT_COMMENT_ALREADY_EXISTS);
        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void 동시에_들어온_중복_신고도_409() {
        stubTargets();
        doThrow(new DuplicateKeyException("dup")).when(reportMapper).insertReport(any(Report.class));

        assertError(() -> service.create(MBR, new ReportCreateRequest(PST, null, "CMUC0009", null)),
                ErrorCode.REPORT_ALREADY_EXISTS);
    }
}
