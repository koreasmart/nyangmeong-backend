package com.nyangmeong.global.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 공통코드 조회]
 */
class CommonCodeServiceTest {

    private final CommonCodeMapper commonCodeMapper = mock(CommonCodeMapper.class);
    private final CommonCodeService commonCodeService = new CommonCodeService(commonCodeMapper);

    @Test
    void 그룹이_있으면_하위_코드를_돌려준다() {
        CommonCodeSummary head = new CommonCodeSummary();
        head.setComCd("MYPC0001");
        when(commonCodeMapper.existsGroup("MYPG01")).thenReturn(true);
        when(commonCodeMapper.selectChildren("MYPG01")).thenReturn(List.of(head));

        assertThat(commonCodeService.search("MYPG01")).extracting(CommonCodeSummary::getComCd).containsExactly("MYPC0001");
    }

    @Test
    void 그룹이_없으면_COMMON_CODE_NOT_FOUND() {
        when(commonCodeMapper.existsGroup("XXXX01")).thenReturn(false);

        assertThatThrownBy(() -> commonCodeService.search("XXXX01"))
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_CODE_NOT_FOUND);
    }
}
