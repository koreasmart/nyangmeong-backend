package com.nyangmeong.seed.domain;

import java.util.List;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [기준 정보]
 * - SYS_STNG, TRMS, RGN(시도·시군구), VRTY, CARE_MSN, ITEM, CMNTY_AI_CMTRY
 * - 담당 도메인이 정식 시드 SQL을 db/에 넣으면 그 테이블은 여기서 뺀다
 */
public class MasterSeeder implements Seeder {

    private record Sido(String code, String name, double lat, double lot, List<Sgg> sggs) {
    }

    private record Sgg(String code, String name, double lat, double lot) {
    }

    /** 행정구역코드 앞 2·5자리와 대략의 중심 좌표 */
    private static final List<Sido> SIDOS = List.of(
            new Sido("11", "서울특별시", 37.5665, 126.9780, List.of(new Sgg("11110", "종로구", 37.5735, 126.9790),
                    new Sgg("11140", "중구", 37.5638, 126.9976), new Sgg("11170", "용산구", 37.5324, 126.9900),
                    new Sgg("11200", "성동구", 37.5634, 127.0369), new Sgg("11215", "광진구", 37.5385, 127.0823),
                    new Sgg("11230", "동대문구", 37.5744, 127.0396), new Sgg("11260", "중랑구", 37.6063, 127.0925),
                    new Sgg("11290", "성북구", 37.5894, 127.0167), new Sgg("11305", "강북구", 37.6396, 127.0257),
                    new Sgg("11320", "도봉구", 37.6688, 127.0471), new Sgg("11350", "노원구", 37.6542, 127.0568),
                    new Sgg("11380", "은평구", 37.6027, 126.9291), new Sgg("11410", "서대문구", 37.5791, 126.9368),
                    new Sgg("11440", "마포구", 37.5663, 126.9019), new Sgg("11470", "양천구", 37.5170, 126.8664),
                    new Sgg("11500", "강서구", 37.5509, 126.8495), new Sgg("11530", "구로구", 37.4954, 126.8874),
                    new Sgg("11545", "금천구", 37.4569, 126.8955), new Sgg("11560", "영등포구", 37.5264, 126.8962),
                    new Sgg("11590", "동작구", 37.5124, 126.9393), new Sgg("11620", "관악구", 37.4784, 126.9516),
                    new Sgg("11650", "서초구", 37.4837, 127.0324), new Sgg("11680", "강남구", 37.5172, 127.0473),
                    new Sgg("11710", "송파구", 37.5145, 127.1059), new Sgg("11740", "강동구", 37.5301, 127.1238))),
            new Sido("26", "부산광역시", 35.1796, 129.0756, List.of(new Sgg("26230", "부산진구", 35.1629, 129.0532),
                    new Sgg("26260", "동래구", 35.2049, 129.0837), new Sgg("26290", "남구", 35.1366, 129.0843),
                    new Sgg("26350", "해운대구", 35.1631, 129.1636))),
            new Sido("27", "대구광역시", 35.8714, 128.6014, List.of(new Sgg("27260", "수성구", 35.8581, 128.6309),
                    new Sgg("27290", "달서구", 35.8298, 128.5328))),
            new Sido("28", "인천광역시", 37.4563, 126.7052, List.of(new Sgg("28185", "연수구", 37.4100, 126.6783),
                    new Sgg("28237", "부평구", 37.5070, 126.7219), new Sgg("28260", "서구", 37.5455, 126.6760))),
            new Sido("29", "광주광역시", 35.1595, 126.8526, List.of(new Sgg("29170", "북구", 35.1740, 126.9120),
                    new Sgg("29200", "광산구", 35.1396, 126.7937))),
            new Sido("30", "대전광역시", 36.3504, 127.3845, List.of(new Sgg("30170", "서구", 36.3555, 127.3838),
                    new Sgg("30200", "유성구", 36.3624, 127.3562))),
            new Sido("31", "울산광역시", 35.5384, 129.3114, List.of(new Sgg("31140", "남구", 35.5438, 129.3301))),
            new Sido("36", "세종특별자치시", 36.4800, 127.2890, List.of(new Sgg("36110", "세종시", 36.4800, 127.2890))),
            new Sido("41", "경기도", 37.4138, 127.5183, List.of(new Sgg("41110", "수원시", 37.2636, 127.0286),
                    new Sgg("41130", "성남시", 37.4200, 127.1265), new Sgg("41170", "안양시", 37.3943, 126.9568),
                    new Sgg("41190", "부천시", 37.5034, 126.7660), new Sgg("41270", "안산시", 37.3219, 126.8309),
                    new Sgg("41280", "고양시", 37.6584, 126.8320), new Sgg("41290", "과천시", 37.4292, 126.9876),
                    new Sgg("41360", "남양주시", 37.6360, 127.2165), new Sgg("41410", "군포시", 37.3617, 126.9352),
                    new Sgg("41460", "용인시", 37.2411, 127.1776), new Sgg("41570", "김포시", 37.6153, 126.7156),
                    new Sgg("41590", "화성시", 37.1995, 126.8312))),
            new Sido("43", "충청북도", 36.6357, 127.4917, List.of(new Sgg("43110", "청주시", 36.6424, 127.4890))),
            new Sido("44", "충청남도", 36.6588, 126.6728, List.of(new Sgg("44130", "천안시", 36.8151, 127.1139))),
            new Sido("46", "전라남도", 34.8161, 126.4629, List.of(new Sgg("46110", "목포시", 34.8118, 126.3922),
                    new Sgg("46130", "여수시", 34.7604, 127.6622))),
            new Sido("47", "경상북도", 36.4919, 128.8889, List.of(new Sgg("47110", "포항시", 36.0190, 129.3435))),
            new Sido("48", "경상남도", 35.4606, 128.2132, List.of(new Sgg("48120", "창원시", 35.2280, 128.6811))),
            new Sido("50", "제주특별자치도", 33.4996, 126.5312, List.of(new Sgg("50110", "제주시", 33.4996, 126.5312),
                    new Sgg("50130", "서귀포시", 33.2541, 126.5600))),
            new Sido("51", "강원특별자치도", 37.8228, 128.1555, List.of(new Sgg("51110", "춘천시", 37.8813, 127.7298),
                    new Sgg("51150", "강릉시", 37.7519, 128.8761))),
            new Sido("52", "전북특별자치도", 35.7175, 127.1530, List.of(new Sgg("52110", "전주시", 35.8242, 127.1480))));

    static final List<String> DOG_BREEDS = List.of("말티즈", "푸들", "포메라니안", "비숑 프리제", "시츄", "치와와", "요크셔 테리어",
            "말티푸", "페키니즈", "닥스훈트", "웰시 코기", "시바견", "비글", "프렌치 불독", "퍼그", "슈나우저", "스피츠", "진돗개",
            "보더 콜리", "사모예드", "시베리안 허스키", "골든 리트리버", "래브라도 리트리버");

    static final List<String> CAT_BREEDS = List.of("코리안 숏헤어", "러시안 블루", "페르시안", "스코티시 폴드", "브리티시 숏헤어", "먼치킨",
            "랙돌", "벵갈", "샴", "아비시니안", "노르웨이 숲", "메인쿤", "터키시 앙고라", "스핑크스");

    private record Mission(String pool, String name, int point, int times, String path) {
    }

    /** 케어미션 마스터 (FN-MYP-020-03·05, 케어리스트는 FN-HLT-003-12 기본 항목, db/migration/20261027_02와 같음) */
    private static final List<Mission> MISSIONS = List.of(
            new Mission("일상케어", "화장실 청소하기", 10, 1, "/health/calendar"),
            new Mission("일상케어", "양치하기", 5, 1, "/health/calendar"),
            new Mission("일상케어", "빗질하기", 5, 1, "/health/calendar"),
            new Mission("일상케어", "발 관리하기", 3, 1, "/health/calendar"),
            new Mission("일상케어", "놀이하기", 5, 1, "/health/calendar"),
            new Mission("일상케어", "체중 기록하기", 10, 1, "/health"),
            new Mission("일상케어", "식사량 기록하기", 10, 1, "/health"),
            new Mission("일상케어", "음수량 기록하기", 10, 1, "/health"),
            new Mission("일상케어", "배변 기록하기", 5, 1, "/health"),
            new Mission("활동기반", "가계부 지출 등록하기", 5, 1, "/mypage/expenses"),
            new Mission("활동기반", "케어 캘린더 일정 등록하기", 5, 1, "/health/calendar"),
            new Mission("활동기반", "귀 청소하기", 10, 1, "/health/calendar"),
            new Mission("활동기반", "발톱 관리하기", 15, 1, "/health/calendar"),
            new Mission("활동기반", "목욕하기", 20, 1, "/health/calendar"),
            new Mission("활동기반", "눈 주변 관리하기", 5, 1, "/health/calendar"),
            new Mission("활동기반", "발바닥 털 관리하기", 10, 1, "/health/calendar"),
            new Mission("활동기반", "구강 상태 확인하기", 5, 1, "/health/calendar"),
            new Mission("활동기반", "산책 기록하기", 10, 1, "/health"),
            new Mission("활동기반", "관찰 기록 추가하기", 10, 1, "/health"),
            new Mission("활동기반", "펫이야기 글 등록하기", 20, 1, "/community"),
            new Mission("활동기반", "펫이야기 댓글 작성하기", 5, 1, "/community"));

    private static final List<List<String>> ITEMS = List.of(
            List.of("머리", "빨간 리본", "밀짚모자", "작은 왕관", "털모자"),
            List.of("얼굴", "동그란 안경", "선글라스", "하트 안경", "외알 안경"),
            List.of("목", "방울 목걸이", "체크 스카프", "나비넥타이", "진주 목걸이"),
            List.of("의상", "노란 우비", "줄무늬 티셔츠", "꼬까 한복", "곰돌이 후드티"),
            List.of("가방", "작은 배낭", "크로스백", "도시락 가방", "꽃바구니"),
            List.of("신발", "빨간 장화", "운동화", "털 부츠", "샌들"));

    private static final List<String> AI_COMMENTS = List.of("오늘은 산책하기 좋은 날씨예요. 짧게라도 바깥 냄새를 맡게 해 주세요.",
            "환절기에는 물을 충분히 마시는지 살펴봐 주세요.", "꾸준한 양치 습관이 건강한 노년을 만들어요.",
            "오늘 기록한 체중을 지난주와 비교해 보세요.", "실내 놀이 10분이 스트레스를 줄여 줘요.", "새 간식을 줄 때는 조금씩 시작해 주세요.");

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        s.store().add("SYS_STNG", r -> r.set("LOWST_HRWG_AMT", 10320).set("REG_DT", s.now().minusDays(200)));

        terms(s);
        regions(s, w, c);
        varieties(s, w, c);

        for (int i = 0; i < MISSIONS.size(); i++) {
            Mission m = MISSIONS.get(i);
            s.store().add("CARE_MSN", r -> r.set("MSN_POOL_CD", c.of("MYPG04", m.pool())).set("MSN_NM", m.name())
                    .set("CMPN_PNT", m.point()).set("ACHV_CRTR_NMTM", m.times()).set("MVMN_PATH", m.path())
                    .set("REG_DT", s.now().minusDays(200)));
        }

        int sort = 0;
        for (List<String> region : ITEMS) {
            String regionCd = c.of("MYPG01", region.get(0));
            for (String name : region.subList(1, region.size())) {
                long fileSn = s.images().add(SeedImages.Kind.ITEM);
                int seq = sort += 10;
                s.store().add("ITEM", r -> r.set("ATCH_FILE_SN", fileSn).set("ITEM_REGN_CD", regionCd).set("ITEM_NM", name)
                        .set("NEED_PNT", s.between(2, 16) * 50).set("SORT_SEQ", seq).yn("NTSL_YN", !s.chance(0.08))
                        .set("REG_DT", s.now().minusDays(200)));
            }
        }

        for (int d = 0; d < 30; d++) {
            boolean ai = s.chance(0.8);
            int day = d;
            s.store().add("CMNTY_AI_CMTRY", r -> r.set("CMTRY_SRC_CD", c.of("AIMG08", ai ? "AI 생성" : "기본 문구"))
                    .set("CMTRY_CRTR_YMD", s.today().minusDays(day))
                    .set("CMTRY_CN", ai ? s.pick(AI_COMMENTS) : "오늘도 반려동물과 행복한 하루 보내세요.")
                    .set("REG_DT", s.today().minusDays(day).atTime(6, 0)));
        }
    }

    private void terms(Seed s) {
        String[][] terms = { { "SERVICE", "서비스 이용약관", "Y" }, { "PRIVACY", "개인정보 수집·이용 동의", "Y" },
                { "AGE_14", "만 14세 이상입니다", "Y" }, { "MARKETING", "마케팅 정보 수신 동의", "N" } };
        for (int i = 0; i < terms.length; i++) {
            String[] t = terms[i];
            int seq = (i + 1) * 10;
            s.store().add("TRMS", r -> r.set("TRMS_FIX_IDNTF_KEY", t[0]).set("TRMS_VER_ID", "v1.0").set("TRMS_TTL", t[1])
                    .set("TRMS_CN", "AGE_14".equals(t[0]) ? t[1] : t[1] + " 본문입니다. 시드 데이터용 문구로 실제 약관이 아닙니다.").yn("ESNTL_AGRE_YN", "Y".equals(t[2]))
                    .set("SORT_SEQ", seq).set("REG_DT", s.now().minusDays(200)));
        }
    }

    private void regions(Seed s, World w, Codes c) {
        String sidoStep = c.of("RGNG01", "시도");
        String sggStep = c.of("RGNG01", "시군구");
        int sort = 0;
        for (Sido sido : SIDOS) {
            int sidoSort = sort += 10;
            Row parent = s.store().add("RGN", r -> r.set("ADMDST_CD", sido.code() + "00000000").set("CTPV_NM", sido.name())
                    .set("SGG_NM", "").set("SORT_SEQ", sidoSort).set("RGN_STP_CD", sidoStep));
            int sggSort = 0;
            for (Sgg sgg : sido.sggs()) {
                int seq = sggSort += 10;
                Row row = s.store().add("RGN", r -> r.set("ADMDST_CD", sgg.code() + "00000").set("CTPV_NM", sido.name())
                        .set("SGG_NM", sgg.name()).set("SORT_SEQ", seq).set("RGN_STP_CD", sggStep)
                        .set("UP_RGN_SN", parent.sn()));
                w.regions.add(new World.Region(row.sn(), sido.name(), sgg.name(), sgg.lat(), sgg.lot()));
            }
        }
    }

    private void varieties(Seed s, World w, Codes c) {
        addVarieties(s, w, c.of("PETG01", "강아지"), "DOGV", DOG_BREEDS, w.dogVarieties);
        addVarieties(s, w, c.of("PETG01", "고양이"), "CATV", CAT_BREEDS, w.catVarieties);
    }

    private void addVarieties(Seed s, World w, String petSeCd, String prefix, List<String> names, List<Long> into) {
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String code = prefix + String.format("%04d", i + 1);
            int seq = (i + 1) * 10;
            Row row = s.store().add("VRTY", r -> r.set("VRTY_CD", code).set("PET_SE_CD", petSeCd).set("VRTY_NM", name)
                    .set("SORT_SEQ", seq));
            into.add(row.sn());
            w.varietyNames.put(row.sn(), name);
        }
    }
}
