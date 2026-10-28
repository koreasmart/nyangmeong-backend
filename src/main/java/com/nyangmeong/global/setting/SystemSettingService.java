package com.nyangmeong.global.setting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * [시스템 설정 조회]
 * - SYS_STNG 1행, 행이 없으면 DDL 기본값
 */
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingMapper systemSettingMapper;

    /**
     * 시스템 설정 조회
     */
    @Transactional(readOnly = true)
    public SystemSetting get() {
        SystemSetting setting = systemSettingMapper.selectCurrent();
        return setting != null ? setting : new SystemSetting();
    }
}
