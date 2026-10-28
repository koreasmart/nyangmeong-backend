package com.nyangmeong.global.setting;

import org.apache.ibatis.annotations.Mapper;

/**
 * [시스템 설정 Mapper]
 */
@Mapper
public interface SystemSettingMapper {

    /**
     * 현재 시스템 설정 조회
     */
    SystemSetting selectCurrent();
}
