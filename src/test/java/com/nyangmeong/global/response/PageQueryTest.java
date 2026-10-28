package com.nyangmeong.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 목록 요청 조건]
 */
class PageQueryTest {

    @Test
    void 생략하면_page_1_size_20이다() {
        PageQuery pq = new PageQuery(null, null, null);

        assertThat(pq.pageOrDefault()).isEqualTo(1);
        assertThat(pq.sizeOrDefault()).isEqualTo(20);
    }

    @Test
    void 화면이_정한_기본_건수를_sizeOr로_넘긴다() {
        PageQuery pq = new PageQuery(2, null, "latest");

        assertThat(pq.pageOrDefault()).isEqualTo(2);
        assertThat(pq.sizeOr(10)).isEqualTo(10);
        assertThat(pq.sort()).isEqualTo("latest");
    }

    @Test
    void size는_50을_넘지_못한다() {
        assertThat(new PageQuery(1, 200, null).sizeOrDefault()).isEqualTo(50);
        assertThat(new PageQuery(1, 50, null).sizeOrDefault()).isEqualTo(50);
        assertThat(new PageQuery(1, 7, null).sizeOr(10)).isEqualTo(7);
    }

    @Test
    void 영이나_음수는_기본값으로_본다() {
        assertThat(new PageQuery(0, 0, null).pageOrDefault()).isEqualTo(1);
        assertThat(new PageQuery(-1, -5, null).sizeOrDefault()).isEqualTo(20);
    }
}
