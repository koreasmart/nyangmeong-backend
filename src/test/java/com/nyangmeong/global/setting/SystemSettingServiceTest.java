package com.nyangmeong.global.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 시스템 설정 조회]
 */
class SystemSettingServiceTest {

    private final SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
    private final SystemSettingService systemSettingService = new SystemSettingService(systemSettingMapper);

    @Test
    void DB_설정값을_돌려준다() {
        SystemSetting setting = new SystemSetting();
        setting.setFileMaxSz(5_242_880L);
        when(systemSettingMapper.selectCurrent()).thenReturn(setting);

        assertThat(systemSettingService.get().getFileMaxSz()).isEqualTo(5_242_880L);
    }

    @Test
    void 설정_행이_없으면_DDL_기본값() {
        when(systemSettingMapper.selectCurrent()).thenReturn(null);

        SystemSetting setting = systemSettingService.get();

        assertThat(setting.getFileMaxSz()).isEqualTo(10_485_760L);
        assertThat(setting.getPetMaxCnt()).isEqualTo(10L);
    }
}
