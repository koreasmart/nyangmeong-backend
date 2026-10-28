package com.nyangmeong.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 알림 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 안 읽음 우선·최신순 정렬과 건수 제한 (FN-COM-004-05)
 */
class NotificationMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/notification/NotificationMapper.xml");

    private static String sql(String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(XML.toFile()).getDocumentElement();
        NodeList selects = mapper.getElementsByTagName("select");
        for (int i = 0; i < selects.getLength(); i++) {
            Element select = (Element) selects.item(i);
            if (id.equals(select.getAttribute("id"))) {
                return select.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("select 없음: " + id);
    }

    @Test
    void 목록은_읽은_알림이_더_최신이어도_안_읽은_알림을_먼저_준다() throws Exception {
        String sql = sql("selectRecentList");

        assertThat(sql).contains("ORDER BY (PRSL_DT IS NULL) DESC");
        assertThat(sql.indexOf("(PRSL_DT IS NULL) DESC")).isLessThan(sql.indexOf("REG_DT DESC"));
    }

    @Test
    void 목록은_같은_읽음_상태끼리_최신순으로_정렬한다() throws Exception {
        assertThat(sql("selectRecentList"))
                .contains("ORDER BY (PRSL_DT IS NULL) DESC , REG_DT DESC , INAPP_NOTI_SN DESC");
    }

    @Test
    void 목록은_정렬한_뒤_건수를_제한해_안_읽은_알림이_최신_5건_밖이어도_포함한다() throws Exception {
        String sql = sql("selectRecentList");

        assertThat(sql).contains("LIMIT #{limit}");
        assertThat(sql.indexOf("ORDER BY")).isLessThan(sql.indexOf("LIMIT"));
    }

    @Test
    void 목록은_7일_경과_숨김과_탭_필터를_그대로_거른다() throws Exception {
        assertThat(sql("selectRecentList")).contains("MBR_SN = #{mbrSn}")
                .contains("AUTO_HIDE_DT > NOW()")
                .contains("NOTI_TYPE_CD IN");
    }
}
