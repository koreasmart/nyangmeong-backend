package com.nyangmeong.seed.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.global.file.FileRootResolver;
import com.nyangmeong.seed.SeedException;
import com.nyangmeong.seed.SeedOptions.Target;

/**
 * [접속·키 설정]
 * - 서버와 같은 파일을 읽는다. 공용은 application.properties, 로컬은 그 위에 application-local.properties
 */
public record SeedSettings(String jdbcUrl, String username, String password, String host, String database,
        String aesKeyBase64, String hmacKeyBase64, Path fileRootDir, String seedPassword) {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Pattern MYSQL_URL = Pattern.compile("jdbc:(?:log4jdbc:)?mysql://([^:/?]+)(?::\\d+)?/([^?]+).*");

    public static SeedSettings load(Target target) {
        Properties props = read(RESOURCES.resolve("application.properties"), true);
        if (target == Target.LOCAL) {
            props.putAll(read(RESOURCES.resolve("application-local.properties"), true));
        }
        String url = required(props, "spring.datasource.url");
        Matcher m = MYSQL_URL.matcher(url);
        if (!m.matches()) {
            throw new SeedException("spring.datasource.url이 jdbc:mysql://주소/DB 형식이 아닙니다");
        }
        // 시드는 MySQL 드라이버로 직접 연결하므로 서버용 log4jdbc 접두어는 뗀다. 대량 INSERT는 여러 행 한 문장으로 묶는다
        String plainUrl = url.replaceFirst("^jdbc:log4jdbc:", "jdbc:");
        String jdbcUrl = plainUrl + (plainUrl.contains("?") ? "&" : "?") + "rewriteBatchedStatements=true";
        return new SeedSettings(jdbcUrl, required(props, "spring.datasource.username"),
                props.getProperty("spring.datasource.password", ""), m.group(1), m.group(2),
                required(props, "app.crypto.aes-key-base64"), required(props, "app.crypto.hmac-key-base64"),
                FileRootResolver.resolve(props.getProperty("app.file.root-dir"), false), props.getProperty("app.seed.password"));
    }

    /**
     * 시드 계정 비밀번호(app.seed.password). 공개 저장소라 코드·README에 두지 않는다
     */
    public String requiredSeedPassword() {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new SeedException("app.seed.password 값이 없습니다. 디스코드의 최신 application.properties를 받아 주세요");
        }
        return seedPassword;
    }

    public boolean isLocalHost() {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public String describeDatabase() {
        return host + "/" + database;
    }

    private static Properties read(Path path, boolean requiredFile) {
        Properties props = new Properties();
        if (!Files.isRegularFile(path)) {
            if (requiredFile) {
                throw new SeedException(path + " 파일이 없습니다. 디스코드의 최신 파일을 받아 주세요");
            }
            return props;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props;
        } catch (IOException e) {
            throw new SeedException(path + "을 읽지 못했습니다: " + e.getMessage());
        }
    }

    private static String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SeedException(key + " 값이 없습니다. application.properties를 확인해 주세요");
        }
        return value.trim();
    }
}
