package com.nyangmeong.ai.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - AI 대화 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 본인 대화만 다루는 조건과 정렬, 잠금 조회 (FN-AIM-011-09·13)
 */
class AiConversationMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/ai/AiConversationMapper.xml");

    private static String sql(String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(XML.toFile()).getDocumentElement();
        for (String tag : new String[] { "select", "delete" }) {
            NodeList nodes = mapper.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                if (id.equals(node.getAttribute("id"))) {
                    return node.getTextContent().replaceAll("\\s+", " ");
                }
            }
        }
        throw new AssertionError("구문 없음: " + id);
    }

    @Test
    void 목록은_본인_대화를_최신순으로_준다() throws Exception {
        assertThat(sql("selectRoomList")).contains("WHERE MBR_SN = ?".replace("?", "#{mbrSn}"))
                .contains("ORDER BY REG_DT DESC , AI_CNVRS_ROOM_SN DESC");
    }

    @Test
    void 목록은_반려동물_태그가_있을_때만_걸러낸다() throws Exception {
        assertThat(sql("selectRoomList")).contains("AND PET_SN = #{petSn}");
    }

    @Test
    void 단건_조회와_삭제는_회원_번호도_함께_비교한다() throws Exception {
        assertThat(sql("selectRoom")).contains("AND MBR_SN = #{mbrSn}");
        assertThat(sql("deleteRoom")).contains("AND MBR_SN = #{mbrSn}");
    }

    @Test
    void 메시지_순서를_매기는_조회는_대화방_행을_잠근다() throws Exception {
        assertThat(sql("selectRoomForUpdate")).contains("AND MBR_SN = #{mbrSn}").endsWith("FOR UPDATE ");
    }

    @Test
    void 메시지는_순서_오름차순으로_준다() throws Exception {
        assertThat(sql("selectMessageList")).contains("ORDER BY MSG_SEQ ASC");
    }
}
