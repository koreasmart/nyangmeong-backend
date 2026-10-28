package com.nyangmeong.global.code;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [공통코드 컨트롤러]
 * - 예: 아이템 부위 탭 ?prntComCd=MYPG01 (FN-MYP-010-14)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/common-codes")
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    @Operation(summary = "공통코드 하위 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommonCodeSummary>>> search(@Valid @ModelAttribute CommonCodeSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(commonCodeService.search(cond.getPrntComCd())));
    }
}
