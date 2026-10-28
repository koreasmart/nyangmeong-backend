package com.nyangmeong.global.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [캐릭터 생성 설정]
 * - app.character.generator : fake(가짜 성공), fake-fail(가짜 실패)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.character")
public class CharacterProperties {

    /** 생성기 종류 */
    private String generator = "fake";
}
