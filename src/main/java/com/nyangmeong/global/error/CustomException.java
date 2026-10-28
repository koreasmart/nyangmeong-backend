package com.nyangmeong.global.error;

/**
 * [비즈니스 예외]
 * - 사용 예: throw new CustomException(ErrorCode.PET_NOT_FOUND);
 */
public class CustomException extends BaseException {

    public CustomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
