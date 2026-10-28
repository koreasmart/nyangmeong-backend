package com.nyangmeong.seed;

/**
 * [시드 중단]
 * - 사람이 고칠 수 있는 원인. 메시지만 출력하고 종료 코드 1
 */
public class SeedException extends RuntimeException {

    public SeedException(String message) {
        super(message);
    }
}
