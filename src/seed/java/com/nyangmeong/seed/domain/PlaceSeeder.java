package com.nyangmeong.seed.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [장소]
 * - PLC 배율 × 500, PLC_BKMK 배율 × 1,000
 */
public class PlaceSeeder implements Seeder {

    private static final List<String> CATEGORIES = List.of("병원", "미용", "용품", "동반", "호텔");
    private static final List<String> GROOMING = List.of("펫살롱", "애견미용실", "도그스파", "캣그루밍");
    private static final List<String> SHOPS = List.of("펫마트", "반려동물용품점", "펫스토어", "멍냥상회");
    private static final List<String> CAFES = List.of("애견동반카페", "펫프렌들리 브런치", "도그파크 카페", "반려견 놀이터 카페");
    private static final List<String> HOTELS = List.of("펫호텔", "애견유치원", "펫스테이", "캣호텔");
    private static final List<String> STREETS = List.of("중앙로", "대학로", "공원로", "시장길", "강변로", "역전로", "문화로", "새마을로");

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        List<String> sources = List.of(c.of("PLCG02", "카카오 로컬"), c.of("PLCG02", "공공데이터"), c.of("PLCG02", "관리자등록"));
        for (int i = 1; i <= s.scaled(500); i++) {
            World.Region region = s.pick(w.regions);
            String category = s.weighted(CATEGORIES, 35, 20, 15, 20, 10);
            String source = s.weighted(sources, 60, 30, 10);
            String name = region.sggNm() + " " + switch (category) {
            case "병원" -> s.pick(Fake.HOSPITALS);
            case "미용" -> s.pick(GROOMING);
            case "용품" -> s.pick(SHOPS);
            case "동반" -> s.pick(CAFES);
            default -> s.pick(HOTELS);
            } + " " + i + "호점";
            int index = i;
            s.store().add("PLC", r -> {
                r.set("RGN_SN", region.sn()).set("PLC_CLSF_CD", c.of("PLCG01", category)).set("PLC_SRC_CD", source)
                        .set("PLC_CONM", name)
                        .set("PLC_ADDR", region.ctpvNm() + " " + region.sggNm() + " " + s.pick(STREETS) + " " + s.between(1, 300))
                        .set("RPRS_TELNO", String.format("0%d-%03d-%04d", s.between(2, 70), s.between(200, 999), s.between(0, 9999)))
                        .set("PLC_LAT", s.decimal(region.lat() + s.between(-0.02, 0.02), 10))
                        .set("PLC_LOT", s.decimal(region.lot() + s.between(-0.025, 0.025), 10)).yn("USE_YN", !s.chance(0.03))
                        .set("REG_DT", s.daysAgo(60, 200));
                if (source.equals(sources.get(0))) {
                    String kakaoId = String.valueOf(10_000_000L + index * 37L);
                    r.set("KAKAO_PLC_ID", kakaoId).set("KAKAO_URL_PATH", "https://place.map.kakao.com/" + kakaoId);
                } else if (source.equals(sources.get(1))) {
                    r.set("PBL_DATA_KEY", String.format("PET-%s-%06d", region.sggNm(), index));
                }
            });
        }

        List<Row> places = s.store().rows("PLC");
        List<World.Member> members = w.activeMembers();
        Set<String> used = new HashSet<>();
        for (int made = 0, tries = 0; made < s.scaled(1000) && tries < s.scaled(5000); tries++) {
            World.Member m = made < 8 ? w.member(1) : s.pick(members);
            Row place = s.pick(places);
            if (!used.add(m.sn() + ":" + place.sn())) {
                continue;
            }
            made++;
            s.store().add("PLC_BKMK", r -> r.set("PLC_SN", place.sn()).set("MBR_SN", m.sn()).yn("USE_YN", !s.chance(0.1))
                    .set("REG_DT", s.after(m.joinedAt())));
        }
    }
}
