package com.nyangmeong;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - DEL_YN 조회 조건]
 * - DEL_YN 테이블 select의 DEL_YN = 'N' 조건 : AGENTS.md "코드를 쓰기 전에 꼭 알 것" 5번
 * - Mapper XML만 읽음 : DB 미사용, 실패 시 SQL에 조건 추가
 */
class MapperXmlRuleTest {

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper");

    /** 삭제된 행까지 읽는 조회 ("namespace.id") */
    private static final Set<String> EXCEPTIONS = Set.of(
            "com.nyangmeong.community.mapper.CommentMapper.selectList");

    private static final Pattern TABLE = Pattern.compile("\\b(?:FROM|JOIN)\\s+`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEL_YN_CONDITION = Pattern.compile("DEL_YN`?\\s*=\\s*'N'", Pattern.CASE_INSENSITIVE);

    @Test
    void DEL_YN_테이블을_조회하는_select에는_조건이_있다() throws Exception {
        assertThat(findViolations(MAPPER_DIR, delYnTables(), EXCEPTIONS)).isEmpty();
    }

    @Test
    void 조건이_빠진_select를_찾아낸다() throws Exception {
        Path fixtures = Path.of("src/test/resources/mapper-rule/bad");

        assertThat(findViolations(fixtures, delYnTables(), Set.of())).containsExactlyInAnyOrder(
                "fixture.BadMapper.selectWithoutCondition",
                "fixture.BadMapper.selectJoinWithOneCondition");
    }

    @Test
    void 조건이_있는_select와_예외_목록은_통과한다() throws Exception {
        Path fixtures = Path.of("src/test/resources/mapper-rule/good");

        assertThat(findViolations(fixtures, delYnTables(), Set.of("fixture.GoodMapper.selectIncludingDeleted")))
                .isEmpty();
    }

    /** DEL_YN 컬럼 보유 테이블 (DDL 변경 시 함께 수정) */
    private static Set<String> delYnTables() throws IOException {
        try (InputStream in = MapperXmlRuleTest.class.getResourceAsStream("/del-yn-tables.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    private static List<String> findViolations(Path dir, Set<String> delYnTables, Set<String> exceptions)
            throws Exception {
        List<String> violations = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return violations;
        }
        List<Path> files;
        try (Stream<Path> paths = Files.walk(dir)) {
            files = paths.filter(path -> path.toString().endsWith(".xml")).sorted().toList();
        }
        for (Path file : files) {
            Element mapper = parse(file).getDocumentElement();
            String namespace = mapper.getAttribute("namespace");
            NodeList selects = mapper.getElementsByTagName("select");
            for (int i = 0; i < selects.getLength(); i++) {
                Element select = (Element) selects.item(i);
                String id = namespace + "." + select.getAttribute("id");
                if (!exceptions.contains(id) && isMissingCondition(select.getTextContent(), delYnTables)) {
                    violations.add(id);
                }
            }
        }
        return violations;
    }

    /** FROM·JOIN에 나온 DEL_YN 테이블 수만큼 조건 필요 (<include> SQL은 검사 밖) */
    private static boolean isMissingCondition(String sql, Set<String> delYnTables) {
        int tableCount = 0;
        Matcher table = TABLE.matcher(sql);
        while (table.find()) {
            if (delYnTables.contains(table.group(1).toUpperCase(Locale.ROOT))) {
                tableCount++;
            }
        }
        long conditionCount = DEL_YN_CONDITION.matcher(sql).results().count();
        return conditionCount < tableCount;
    }

    private static Document parse(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        /** MyBatis DTD 외부 다운로드 차단 */
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file.toFile());
    }
}
