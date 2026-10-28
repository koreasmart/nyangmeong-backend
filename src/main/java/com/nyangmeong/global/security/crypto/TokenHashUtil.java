package com.nyangmeong.global.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * [Refresh 토큰 해시 유틸]
 * - SHA-256 해시만 저장 (RFRSH_TKN.RFRSH_TKN_HASH)
 */
@Component
public class TokenHashUtil {

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            /** JDK 표준 알고리즘 */
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e);
        }
    }
}
