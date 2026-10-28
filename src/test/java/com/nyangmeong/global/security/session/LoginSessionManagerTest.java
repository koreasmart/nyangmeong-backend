package com.nyangmeong.global.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.domain.RefreshToken;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenClaims;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - 로그인 세션 발급·회전·폐기]
 * - RFRSH_TKN 은 메모리 가짜 Mapper
 */
class LoginSessionManagerTest {

    /** RFRSH_TKN 메모리 저장소 */
    static class FakeRefreshTokenMapper implements RefreshTokenMapper {

        final List<RefreshToken> rows = new ArrayList<>();

        @Override
        public void insert(RefreshToken token, long expirySeconds) {
            token.setRfrshTknSn((long) rows.size() + 1);
            token.setExpryDt(LocalDateTime.now().plusSeconds(expirySeconds));
            rows.add(token);
        }

        @Override
        public void updateHash(Long rfrshTknSn, String rfrshTknHash) {
            row(rfrshTknSn).ifPresent(r -> r.setRfrshTknHash(rfrshTknHash));
        }

        @Override
        public RefreshToken selectActiveByHash(String rfrshTknHash) {
            return rows.stream()
                    .filter(r -> r.getRfrshTknHash().equals(rfrshTknHash) && r.getDscdDt() == null
                            && r.getExpryDt().isAfter(LocalDateTime.now()))
                    .findFirst().orElse(null);
        }

        @Override
        public int updateDiscard(Long rfrshTknSn) {
            return row(rfrshTknSn).filter(r -> r.getDscdDt() == null).map(this::discard).orElse(0);
        }

        @Override
        public int updateDiscardByHash(String rfrshTknHash) {
            return rows.stream().filter(r -> r.getRfrshTknHash().equals(rfrshTknHash) && r.getDscdDt() == null)
                    .findFirst().map(this::discard).orElse(0);
        }

        private Optional<RefreshToken> row(Long rfrshTknSn) {
            return rows.stream().filter(r -> r.getRfrshTknSn().equals(rfrshTknSn)).findFirst();
        }

        private int discard(RefreshToken row) {
            row.setDscdDt(LocalDateTime.now());
            return 1;
        }
    }

    private FakeRefreshTokenMapper mapper;
    private JwtTokenProvider provider;
    private TokenHashUtil hashUtil;
    private LoginSessionManager manager;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretBase64(Base64.getEncoder().encodeToString("jwt-secret-for-tests-32-bytes-long!!".getBytes()));
        provider = new JwtTokenProvider(properties);
        provider.initializeKey();
        mapper = new FakeRefreshTokenMapper();
        hashUtil = new TokenHashUtil();
        manager = new LoginSessionManager(mapper, provider, properties, new TokenCookies(properties), hashUtil);
    }

    /** Set-Cookie 에서 쿠키 값 */
    private static String cookieValue(MockHttpServletResponse response, String name) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(h -> h.startsWith(name + "="))
                .map(h -> h.substring(name.length() + 1, h.indexOf(';')))
                .reduce((first, second) -> second).orElse(null);
    }

    private static MockHttpServletRequest withRefresh(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.setCookies(new Cookie(TokenCookies.REFRESH_TOKEN, refreshToken));
        return request;
    }

    private String issue(Long mbrSn) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        manager.issue(mbrSn, response);
        return cookieValue(response, TokenCookies.REFRESH_TOKEN);
    }

    private static void assertUnauthorized(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 발급하면_행에는_해시만_남고_두_토큰의_sid는_행_번호다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.issue(7L, response);

        String access = cookieValue(response, TokenCookies.ACCESS_TOKEN);
        String refresh = cookieValue(response, TokenCookies.REFRESH_TOKEN);
        RefreshToken row = mapper.rows.get(0);
        assertThat(row.getMbrSn()).isEqualTo(7L);
        assertThat(row.getRfrshTknHash()).isEqualTo(hashUtil.hash(refresh)).isNotEqualTo(refresh);
        assertThat(provider.parseAccess(access)).isEqualTo(new TokenClaims(7L, row.getRfrshTknSn()));
        assertThat(provider.parseRefresh(refresh)).isEqualTo(new TokenClaims(7L, row.getRfrshTknSn()));
    }

    @Test
    void 회전하면_옛_세션은_폐기되고_새_세션의_쿠키가_나간다() {
        String refresh = issue(7L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Long mbrSn = manager.rotate(withRefresh(refresh), response);

        assertThat(mbrSn).isEqualTo(7L);
        assertThat(mapper.rows).hasSize(2);
        assertThat(mapper.rows.get(0).getDscdDt()).isNotNull();
        assertThat(mapper.rows.get(1).getDscdDt()).isNull();
        String newRefresh = cookieValue(response, TokenCookies.REFRESH_TOKEN);
        assertThat(newRefresh).isNotEqualTo(refresh);
        assertThat(provider.parseRefresh(newRefresh).sid()).isEqualTo(mapper.rows.get(1).getRfrshTknSn());
    }

    @Test
    void 같은_Refresh로_두_번째_회전하면_UNAUTHORIZED와_쿠키_삭제다() {
        String refresh = issue(7L);
        manager.rotate(withRefresh(refresh), new MockHttpServletResponse());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertUnauthorized(() -> manager.rotate(withRefresh(refresh), response));
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2).allMatch(h -> h.contains("Max-Age=0"));
        assertThat(mapper.rows).hasSize(2);
    }

    @Test
    void 폐기한_세션은_다시_회전할_수_없다() {
        String refresh = issue(7L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.revoke(withRefresh(refresh), response);

        assertThat(mapper.rows.get(0).getDscdDt()).isNotNull();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2).allMatch(h -> h.contains("Max-Age=0"));
        assertUnauthorized(() -> manager.rotate(withRefresh(refresh), new MockHttpServletResponse()));
    }

    @Test
    void 쿠키가_없거나_Access_토큰이면_회전하지_않는다() {
        MockHttpServletResponse issued = new MockHttpServletResponse();
        manager.issue(7L, issued);
        String access = cookieValue(issued, TokenCookies.ACCESS_TOKEN);

        assertUnauthorized(() -> manager.rotate(new MockHttpServletRequest(), new MockHttpServletResponse()));
        assertUnauthorized(() -> manager.rotate(withRefresh(access), new MockHttpServletResponse()));
        assertThat(mapper.rows.get(0).getDscdDt()).isNull();
    }

    @Test
    void 쿠키가_없거나_무효여도_로그아웃은_예외_없이_쿠키를_지운다() {
        MockHttpServletResponse noCookie = new MockHttpServletResponse();
        manager.revoke(new MockHttpServletRequest(), noCookie);
        MockHttpServletResponse invalid = new MockHttpServletResponse();
        manager.revoke(withRefresh("not.a.jwt"), invalid);

        assertThat(noCookie.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2).allMatch(h -> h.contains("Max-Age=0"));
        assertThat(invalid.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2).allMatch(h -> h.contains("Max-Age=0"));
    }
}
