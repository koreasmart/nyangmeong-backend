package com.nyangmeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.security.SecurityPathProperties;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * [검사 - Swagger 설정]
 * - 스프링 컨텍스트 없이 OpenAPI 와 customizer 를 직접 적용
 */
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig(new SecurityPathProperties());

    private OpenAPI documentWith(Operation loginPost, Operation meGet, Operation pingPost) {
        OpenAPI openApi = config.openAPI();
        openApi.setPaths(new Paths()
                .addPathItem("/api/auth/login", new PathItem().post(loginPost))
                .addPathItem("/api/auth/me", new PathItem().get(meGet))
                .addPathItem("/api/ping", new PathItem().post(pingPost)));
        return openApi;
    }

    private static long requestedWithCount(Operation operation) {
        if (operation.getParameters() == null) {
            return 0;
        }
        return operation.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()) && "X-Requested-With".equals(p.getName()))
                .count();
    }

    @Test
    void 문서_제목과_쿠키_인증_방식이_있다() {
        OpenAPI openApi = config.openAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("냥멍케어 API");
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.COOKIE_AUTH);
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(scheme.getIn()).isEqualTo(SecurityScheme.In.COOKIE);
        assertThat(scheme.getName()).isEqualTo("access_token");
        assertThat(openApi.getSecurity()).hasSize(1);
    }

    @Test
    void 상태_변경_요청에만_X_Requested_With_헤더를_한_번_붙인다() {
        Operation loginPost = new Operation();
        Operation meGet = new Operation();
        Operation pingPost = new Operation();
        OpenAPI openApi = documentWith(loginPost, meGet, pingPost);

        config.requestedWithAndPermitAllCustomizer().customise(openApi);
        config.requestedWithAndPermitAllCustomizer().customise(openApi);

        assertThat(requestedWithCount(pingPost)).isEqualTo(1);
        assertThat(requestedWithCount(loginPost)).isEqualTo(1);
        assertThat(requestedWithCount(meGet)).isZero();
        Parameter header = pingPost.getParameters().get(0);
        assertThat(header.getRequired()).isTrue();
        assertThat(header.getSchema().getDefault()).isEqualTo("XMLHttpRequest");
        assertThat(header.getExample()).isEqualTo("XMLHttpRequest");
    }

    @Test
    void 열린_경로만_자물쇠를_뺀다() {
        Operation loginPost = new Operation();
        Operation meGet = new Operation();
        Operation pingPost = new Operation();
        OpenAPI openApi = documentWith(loginPost, meGet, pingPost);

        config.requestedWithAndPermitAllCustomizer().customise(openApi);

        assertThat(loginPost.getSecurity()).isEmpty();
        assertThat(meGet.getSecurity()).isNull();
        assertThat(pingPost.getSecurity()).isNull();
    }
}
