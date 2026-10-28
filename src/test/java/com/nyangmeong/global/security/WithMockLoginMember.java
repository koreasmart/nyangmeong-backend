package com.nyangmeong.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * [테스트 - 가짜 로그인 회원]
 * - 기본은 회원 번호 1의 일반회원
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockLoginMemberSecurityContextFactory.class)
public @interface WithMockLoginMember {

    long id() default 1L;

    String mbrAuthrtCd() default "USRC0004";
}
