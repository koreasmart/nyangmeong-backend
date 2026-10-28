package com.nyangmeong.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 표준 API 응답]
 */
class ApiResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void 성공_응답은_status_success와_data를_담고_path_code는_빠진다() {
        JsonNode json = mapper.valueToTree(ApiResponse.success(Map.of("mbrNcnm", "초코보호자")));

        assertThat(json.get("status").asString()).isEqualTo("success");
        assertThat(json.get("httpCode").asInt()).isEqualTo(200);
        assertThat(json.get("message").asString()).isEqualTo("ok");
        assertThat(json.get("data").get("mbrNcnm").asString()).isEqualTo("초코보호자");
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.has("path")).isFalse();
        assertThat(json.has("code")).isFalse();
    }

    @Test
    void data_없는_성공_응답은_data가_빠진다() {
        JsonNode json = mapper.valueToTree(ApiResponse.success());

        assertThat(json.has("data")).isFalse();
    }

    @Test
    void 생성_응답은_201과_created다() {
        JsonNode json = mapper.valueToTree(ApiResponse.created(Map.of("pstSn", 1)));

        assertThat(json.get("httpCode").asInt()).isEqualTo(201);
        assertThat(json.get("message").asString()).isEqualTo("created");
        assertThat(json.get("data").get("pstSn").asInt()).isEqualTo(1);
    }

    @Test
    void 실패_응답은_ErrorCode의_상태_메시지_이름과_path를_담는다() {
        JsonNode json = mapper.valueToTree(ApiResponse.error(ErrorCode.MEMBER_NOT_FOUND, "/api/auth/me"));

        assertThat(json.get("status").asString()).isEqualTo("error");
        assertThat(json.get("httpCode").asInt()).isEqualTo(404);
        assertThat(json.get("message").asString()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.message());
        assertThat(json.get("code").asString()).isEqualTo("MEMBER_NOT_FOUND");
        assertThat(json.get("path").asString()).isEqualTo("/api/auth/me");
        assertThat(json.has("data")).isFalse();
    }

    @Test
    void 메시지를_바꾼_실패_응답도_code와_상태는_ErrorCode를_따른다() {
        JsonNode json = mapper.valueToTree(ApiResponse.error(ErrorCode.BAD_REQUEST, "다른 문구", "/x"));

        assertThat(json.get("message").asString()).isEqualTo("다른 문구");
        assertThat(json.get("code").asString()).isEqualTo("BAD_REQUEST");
        assertThat(json.get("httpCode").asInt()).isEqualTo(400);
    }

    @Test
    void 검증_오류_응답은_필드_목록을_data에_담는다() {
        List<Map<String, String>> fields = List.of(Map.of("field", "mbrNcnm", "message", "이름을 입력해 주세요"));
        JsonNode json = mapper.valueToTree(ApiResponse.errorWithData(ErrorCode.INVALID_INPUT_VALUE, "/x", fields));

        assertThat(json.get("code").asString()).isEqualTo("INVALID_INPUT_VALUE");
        assertThat(json.get("data").get(0).get("field").asString()).isEqualTo("mbrNcnm");
    }
}
