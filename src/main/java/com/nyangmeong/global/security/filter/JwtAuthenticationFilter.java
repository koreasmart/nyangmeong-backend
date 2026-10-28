package com.nyangmeong.global.security.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenClaims;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [JWT 인증 필터]
 * - access_token 쿠키 검증 후 SecurityContext 에 LoginMember 저장
 * - 토큰 없음/무효/만료/폐기된 세션은 미인증 통과 (사유는 요청 속성 AUTH_ERROR_CODE)
 * - 정지/탈퇴 회원은 403 + 쿠키 삭제
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_CODE = JwtAuthenticationFilter.class.getName() + ".errorCode";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookies tokenCookies;
    private final AuthMemberMapper authMemberMapper;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = tokenCookies.read(request, TokenCookies.ACCESS_TOKEN);
        if (token == null || token.isBlank() || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parseAccess(token);
        } catch (BaseException e) {
            request.setAttribute(AUTH_ERROR_CODE, e.getErrorCode());
            chain.doFilter(request, response);
            return;
        }

        AuthMember member = authMemberMapper.selectForSession(claims.mbrSn(), claims.sid());
        if (member == null) {
            /** 회원 행 없음 또는 폐기·만료된 세션 */
            request.setAttribute(AUTH_ERROR_CODE, ErrorCode.UNAUTHORIZED);
            chain.doFilter(request, response);
            return;
        }
        if (MemberStatus.isInactive(member.getMbrSttsCd())) {
            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookies.deleteAccessToken().toString());
            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookies.deleteRefreshToken().toString());
            errorResponseWriter.write(request, response, ErrorCode.MEMBER_INACTIVE);
            return;
        }

        LoginMember loginMember = new LoginMember(member.getMbrSn(), member.getMbrAuthrtCd());
        /** 권한은 DB 값 기준 */
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                loginMember, null,
                List.of(new SimpleGrantedAuthority(MemberAuthority.roleOf(member.getMbrAuthrtCd()))));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
