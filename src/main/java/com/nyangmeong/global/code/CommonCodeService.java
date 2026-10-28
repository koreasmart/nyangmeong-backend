package com.nyangmeong.global.code;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [공통코드 조회]
 */
@Service
@RequiredArgsConstructor
public class CommonCodeService {

    private final CommonCodeMapper commonCodeMapper;

    /**
     * 그룹의 하위 코드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommonCodeSummary> search(String prntComCd) {
        if (!commonCodeMapper.existsGroup(prntComCd)) {
            throw new CustomException(ErrorCode.COMMON_CODE_NOT_FOUND);
        }
        return commonCodeMapper.selectChildren(prntComCd);
    }
}
