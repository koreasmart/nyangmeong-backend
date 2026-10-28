package com.nyangmeong.global.config;

import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import com.nyangmeong.global.security.SecurityPathProperties;
import com.nyangmeong.global.security.filter.RequestedWithFilter;
import com.nyangmeong.global.security.jwt.TokenCookies;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

/**
 * [Swagger(OpenAPI) 설정]
 * - 문서 정보, 쿠키 인증(access_token), 상태 변경 요청의 X-Requested-With 기본값
 * - 열린 경로(app.security.permit-all-paths)는 자물쇠 제외
 * - prod 프로필에서는 application-prod.properties 로 끈다
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    public static final String COOKIE_AUTH = "cookieAuth";

    private static final Set<PathItem.HttpMethod> SAFE_METHODS = Set.of(PathItem.HttpMethod.GET,
            PathItem.HttpMethod.HEAD, PathItem.HttpMethod.OPTIONS);

    private final SecurityPathProperties securityPathProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("냥멍케어 API")
                        .version("0.1")
                        .description("인증은 httpOnly 쿠키(access_token). 상태 변경 요청은 X-Requested-With: XMLHttpRequest 필요. "
                                + "오류 code는 docs/api/<도메인>.md"))
                .components(new Components().addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(TokenCookies.ACCESS_TOKEN)))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
    }

    /**
     * 경로·메서드별 헤더와 자물쇠 조정
     */
    @Bean
    public GlobalOpenApiCustomizer requestedWithAndPermitAllCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (!SAFE_METHODS.contains(method)) {
                    addRequestedWithHeader(operation);
                }
                if (isPermitAll(path)) {
                    /** 빈 목록 = 전역 자물쇠 덮어쓰기 */
                    operation.setSecurity(List.of());
                }
            }));
        };
    }

    private void addRequestedWithHeader(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null && parameters.stream().anyMatch(p -> "header".equals(p.getIn())
                && RequestedWithFilter.HEADER.equalsIgnoreCase(p.getName()))) {
            return;
        }
        StringSchema schema = new StringSchema();
        schema.setDefault(RequestedWithFilter.EXPECTED_VALUE);
        operation.addParametersItem(new HeaderParameter()
                .name(RequestedWithFilter.HEADER)
                .required(true)
                .schema(schema)
                .example(RequestedWithFilter.EXPECTED_VALUE));
    }

    private boolean isPermitAll(String path) {
        return securityPathProperties.getPermitAllPaths().stream().anyMatch(p -> pathMatcher.match(p, path));
    }
}
