package com.nyangmeong.global.security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.domain.MemberAuthority;

/**
 * [테스트 - 가짜 인증 컨텍스트]
 */
public class WithMockLoginMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockLoginMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockLoginMember annotation) {
        LoginMember loginMember = new LoginMember(annotation.id(), annotation.mbrAuthrtCd());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                loginMember, null,
                List.of(new SimpleGrantedAuthority(MemberAuthority.roleOf(annotation.mbrAuthrtCd()))));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
