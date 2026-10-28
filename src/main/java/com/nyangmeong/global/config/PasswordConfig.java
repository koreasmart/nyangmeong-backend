package com.nyangmeong.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * [설정 - 비밀번호 해시]
 * - BCrypt, MBR.PSWD_HASH
 * - SecurityConfig 와 분리 : 서비스 주입 시 순환 참조 방지
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
