package com.nyangmeong.global.code;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * [공통코드 목록 조건]
 */
@Data
public class CommonCodeSearchCond {
    @NotBlank
    @Size(max = 8)
    private String prntComCd;
}
