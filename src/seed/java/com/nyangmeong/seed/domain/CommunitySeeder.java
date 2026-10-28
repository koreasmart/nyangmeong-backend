package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [커뮤니티]
 * - PST 배율 × 1,000(일반 800, 실종 제보 100, 펫돌봄 모집 100), 이미지·좋아요·댓글·신고
 * - 실종 제보·펫돌봄 모집 글의 BBS_CD는 정해진 규칙이 없어 펫일상으로 둔다
 * - 펫돌봄: 모집 → 채팅방(지원자) → 매칭·조건 버전 → 돌봄 완료 요청 → 거래 완료
 */
public class CommunitySeeder implements Seeder {

    private static final int BLIND_REPORTS = 5;

    /** 매칭 거래 상태 → 거쳐 온 상태 */
    private static final Map<String, List<String>> MATCH_PATHS = Map.of(
            "수락 대기", List.of("수락 대기"),
            "요청 취소", List.of("수락 대기", "요청 취소"),
            "무응답 종료", List.of("수락 대기", "무응답 종료"),
            "매칭 완료", List.of("수락 대기", "매칭 완료"),
            "확인 대기", List.of("수락 대기", "매칭 완료", "확인 대기"),
            "수령 대기", List.of("수락 대기", "매칭 완료", "확인 대기", "수령 대기"),
            "거래 완료", List.of("수락 대기", "매칭 완료", "확인 대기", "수령 대기", "거래 완료"));

    private Codes c;
    private FieldCryptoService crypto;
    private final List<Row> blindPosts = new ArrayList<>();
    /** 게시글 → 최상위 댓글 */
    private final Map<Long, List<Long>> topComments = new HashMap<>();

    @Override
    public void run(Seed s, World w) {
        c = s.codes();
        crypto = s.crypto();
        List<World.Member> writers = w.activeMembers();
        for (int i = 0; i < s.scaled(800); i++) {
            World.Member m = i < 12 ? w.member(1) : s.pick(writers);
            generalPost(s, w, m);
        }
        for (int i = 0; i < s.scaled(100); i++) {
            lostReport(s, w, s.pick(writers));
        }
        for (int i = 0; i < s.scaled(100); i++) {
            recruit(s, w, i < 3 ? w.member(1) : s.pick(writers), writers);
        }
        likes(s, w, writers);
        comments(s, w, writers);
        reports(s, w, writers);
    }

    // ---- 게시글

    private Row post(Seed s, World w, World.Member m, String bbs, String title, String content, String status, LocalDateTime at) {
        Row post = s.store().add("PST", r -> r.set("MBR_SN", m.sn()).set("BBS_CD", c.of("CMUG01", bbs))
                .set("PST_STTS_CD", c.of("CMUG02", status)).set("PST_TTL", title).set("PST_CN", content)
                .set("INQ_CNT", s.between(0, 1500)).set("RCMDTN_CNT", 0).set("REG_DT", at));
        postHistory(s, post.sn(), m.sn(), null, "정상", null, at);
        if ("블라인드".equals(status)) {
            postHistory(s, post.sn(), null, "정상", "블라인드", "신고 누적으로 블라인드", s.after(at));
            blindPosts.add(post);
        } else if ("삭제".equals(status)) {
            postHistory(s, post.sn(), m.sn(), "정상", "삭제", "작성자 삭제", s.after(at));
        }
        w.posts.add(new World.Post(post.sn(), m.sn(), at, "정상".equals(status)));
        return post;
    }

    private void generalPost(Seed s, World w, World.Member m) {
        boolean info = s.chance(0.4);
        String status = s.weighted(List.of("정상", "블라인드", "삭제"), 90, 3, 7);
        LocalDateTime at = s.after(later(m.joinedAt(), s.now().minusDays(180)));
        Row post = post(s, w, m, info ? "펫정보" : "펫일상", s.pick(info ? Fake.INFO_TITLES : Fake.DAILY_TITLES),
                Fake.paragraph(s, 2, 6), status, at);
        long infoCount = s.store().rows("PST").stream().filter(p -> p.get("HLTH_INFO_EXPSR_SEQ") != null).count();
        if (info && "정상".equals(status) && infoCount < 10) {
            post.set("HLTH_INFO_EXPSR_SEQ", infoCount + 1);
        }
        if (s.chance(0.4)) {
            for (int k = 0, n = s.between(1, 3); k < n; k++) {
                long fileSn = s.images().add(SeedImages.Kind.POST);
                int seq = (k + 1) * 10;
                s.store().add("PST_IMG", r -> r.set("PST_SN", post.sn()).set("ATCH_FILE_SN", fileSn).set("SORT_SEQ", seq)
                        .yn("DEL_YN", s.chance(0.02)).set("REG_DT", at));
            }
        }
    }

    private void postHistory(Seed s, long pstSn, Long mbrSn, String before, String after, String reason, LocalDateTime at) {
        s.store().add("PST_STTS_HSTRY", r -> r.set("PST_SN", pstSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("CMUG02", before)).set("AFTR_STTS_CD", c.of("CMUG02", after))
                .set("CHG_RSN", reason).set("REG_DT", at));
    }

    // ---- 실종 제보

    private void lostReport(Seed s, World w, World.Member m) {
        World.Region region = s.pick(w.regions);
        boolean dog = s.chance(0.6);
        Long vrtySn = s.chance(0.8) ? s.pick(dog ? w.dogVarieties : w.catVarieties) : null;
        String breed = vrtySn == null ? (dog ? "믹스견" : "코숏") : w.varietyNames.get(vrtySn);
        String petName = s.pick(Fake.PET_NAMES);
        String status = s.weighted(List.of("실종", "찾았어요", "만료"), 60, 25, 15);
        LocalDateTime seenAt = "만료".equals(status) ? s.daysAgo(35, 60) : s.daysAgo(1, 25);
        int renewals = "실종".equals(status) && s.chance(0.2) ? s.between(1, 2) : 0;
        LocalDateTime postedAt = s.soonAfter(seenAt, 60 * 6);
        Row post = post(s, w, m, "펫일상", "[실종] " + region.sggNm() + " " + breed + " " + petName + "를 찾습니다",
                region.sggNm() + " 근처에서 잃어버렸어요. " + Fake.paragraph(s, 1, 2) + " 보신 분은 연락 부탁드립니다.", "정상", postedAt);
        LocalDateTime foundAt = "찾았어요".equals(status) ? s.after(postedAt) : null;
        Row lost = s.store().add("LOST_TPOF", r -> r.set("PST_SN", post.sn()).set("RGN_SN", region.sn())
                .set("PET_SE_CD", c.of("PETG01", dog ? "강아지" : "고양이")).set("LOST_TPOF_STTS_CD", c.of("CMUG03", status))
                .set("PET_NM", petName).set("COLR_NM", s.pick("흰색", "갈색", "검정", "치즈", "얼룩"))
                .set("CNPL_ENC", crypto.encrypt(Fake.phone(5000 + (int) post.sn())))
                .set("WTNS_PLC_LAT", s.decimal(region.lat() + s.between(-0.01, 0.01), 10))
                .set("WTNS_PLC_LOT", s.decimal(region.lot() + s.between(-0.01, 0.01), 10)).set("PSTG_PRD_UPDT_NMTM", renewals)
                .set("LAST_WTNS_DT", seenAt).set("EXPRY_CRTR_DT", seenAt.plusDays(30L * (renewals + 1))).set("DSCV_SWTC_DT", foundAt)
                .set("WTNS_PLC_NM", region.sggNm() + " " + s.pick("근린공원", "역 앞", "놀이터", "하천 산책로")).yn("PRVC_RLS_AGRE_YN", true)
                .set("VRTY_SN", vrtySn).set("VRTY_DRCT_INPT_NM", vrtySn == null ? breed : null)
                .set("CHRC_CN", s.pick("빨간 목줄", "왼쪽 귀에 점", "꼬리가 짧아요", "겁이 많아요")).set("REG_DT", postedAt));
        lostHistory(s, lost.sn(), m.sn(), null, "실종", postedAt);
        if (foundAt != null) {
            lostHistory(s, lost.sn(), m.sn(), "실종", "찾았어요", foundAt);
            String message = "🎉 " + petName + "가 무사히 가족 품으로 돌아왔어요!";
            s.store().add("CMNT", r -> r.set("PST_SN", post.sn()).set("MBR_SN", m.sn()).set("CMNT_CN", message)
                    .yn("SYS_AUTO_CRT_YN", true).set("REG_DT", foundAt));
        } else if ("만료".equals(status)) {
            lostHistory(s, lost.sn(), null, "실종", "만료", seenAt.plusDays(30));
        }
    }

    private void lostHistory(Seed s, long lostSn, Long mbrSn, String before, String after, LocalDateTime at) {
        s.store().add("LOST_TPOF_STTS_HSTRY", r -> r.set("LOST_TPOF_SN", lostSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("CMUG03", before)).set("AFTR_STTS_CD", c.of("CMUG03", after))
                .set("REG_DT", at));
    }

    // ---- 펫돌봄 모집

    private void recruit(Seed s, World w, World.Member author, List<World.Member> people) {
        World.Region region = s.pick(w.regions);
        String status = s.weighted(List.of("모집중", "매칭완료", "거래완료"), 50, 25, 25);
        List<String> shapes = s.sample(c.all("PCAG03"), s.between(1, 3));
        String shapeName = c.name("PCAG03", shapes.get(0));
        LocalDateTime postedAt = "모집중".equals(status) ? s.daysAgo(0, 20) : s.daysAgo(20, 90);
        Row post = post(s, w, author, "펫일상", "[펫돌봄] " + region.sggNm() + " " + shapeName + " 해 주실 분 구해요",
                Fake.paragraph(s, 2, 4), "정상", postedAt);
        boolean hourly = s.chance(0.5);
        boolean negotiable = s.chance(0.3);
        LocalDateTime begin = negotiable ? null : postedAt.plusDays(s.between(2, 10)).withHour(s.between(8, 14)).withMinute(0);
        Row rcrt = s.store().add("PET_PRTC_RCRT", r -> r.set("PST_SN", post.sn()).set("RGN_SN", region.sn())
                .set("RCRT_STTS_CD", c.of("PCAG01", status)).set("CST_UNIT_CD", c.of("PCAG02", hourly ? "시급" : "건당"))
                .set("CST_AMT", hourly ? s.between(1032, 2000) * 10 : s.between(2, 8) * 10_000).set("RLS_RGN_NM", region.sggNm())
                .set("PRTC_PLC_DADDR_ENC", crypto.encrypt(region.ctpvNm() + " " + region.sggNm() + " 행복로 " + s.between(1, 99) + ", "
                        + s.between(101, 1203) + "호"))
                .set("PRTC_BGNG_DT", begin).set("PRTC_END_DT", begin == null ? null : begin.plusHours(s.between(2, 72)))
                .yn("SCHDL_CNSLTN_PSBLTY_YN", negotiable).yn("UTZTN_RUL_AGRE_YN", true)
                .set("PRTC_PLC_LAT", s.decimal(region.lat() + s.between(-0.01, 0.01), 10))
                .set("PRTC_PLC_LOT", s.decimal(region.lot() + s.between(-0.01, 0.01), 10)).set("REG_DT", postedAt));
        recruitHistory(s, rcrt.sn(), author.sn(), null, "모집중", postedAt);

        recruitPets(s, w, author, rcrt.sn(), postedAt);
        for (String shape : shapes) {
            s.store().add("RCRT_PRTC_SHP", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("PRTC_SHP_CD", shape).set("REG_DT", postedAt));
        }

        List<World.Member> applicants = s.sample(people.stream().filter(p -> p.sn() != author.sn()).toList(), s.between(1, 4));
        for (int k = 0; k < applicants.size(); k++) {
            World.Member applicant = applicants.get(k);
            boolean confirmed = k == 0 && !"모집중".equals(status);
            String matchStatus = confirmed ? ("거래완료".equals(status) ? "거래 완료" : s.pick("매칭 완료", "확인 대기", "수령 대기"))
                    : "모집중".equals(status) && s.chance(0.4) ? s.pick("수락 대기", "요청 취소", "무응답 종료") : null;
            LocalDateTime chatAt = s.after(postedAt);
            long roomSn = chatRoom(s, post.sn(), author, applicant, chatAt, matchStatus, confirmed, "거래완료".equals(status));
            if (matchStatus == null) {
                continue;
            }
            Row match = match(s, rcrt, roomSn, author, applicant, matchStatus, chatAt);
            if (confirmed) {
                rcrt.set("CFMTN_PET_PRTC_MTCH_SN", match.sn());
                LocalDateTime matchedAt = (LocalDateTime) match.get("REG_DT");
                recruitHistory(s, rcrt.sn(), author.sn(), "모집중", "매칭완료", s.soonAfter(matchedAt, 60));
                if ("거래완료".equals(status)) {
                    recruitHistory(s, rcrt.sn(), author.sn(), "매칭완료", "거래완료", (LocalDateTime) match.get("RCV_IDNTY_DT"));
                }
            }
        }
    }

    private void recruitPets(Seed s, World w, World.Member author, long rcrtSn, LocalDateTime at) {
        List<Long> own = w.caredPets.getOrDefault(author.sn(), List.of());
        Set<Boolean> species = new HashSet<>();
        for (Long petSn : s.sample(own, 2)) {
            World.Pet pet = w.pet(petSn);
            if (species.add(pet.dog())) {
                addRecruitPet(s, rcrtSn, pet.dog(), pet.weight().doubleValue(), pet.vrtySn(), at);
            }
        }
        if (species.isEmpty()) {
            boolean dog = s.chance(0.6);
            addRecruitPet(s, rcrtSn, dog, s.between(2.0, 12.0), null, at);
        }
    }

    private void addRecruitPet(Seed s, long rcrtSn, boolean dog, double weight, Long vrtySn, LocalDateTime at) {
        s.store().add("RCRT_PET_KND", r -> r.set("PET_PRTC_RCRT_SN", rcrtSn).set("PET_SE_CD", c.of("PETG01", dog ? "강아지" : "고양이"))
                .set("PET_BDWG", s.decimal(weight, 3)).set("VRTY_SN", vrtySn)
                .set("VRTY_DRCT_INPT_NM", vrtySn == null ? (dog ? "믹스견" : "믹스묘") : null).set("REG_DT", at));
    }

    private void recruitHistory(Seed s, long rcrtSn, long mbrSn, String before, String after, LocalDateTime at) {
        s.store().add("PET_PRTC_RCRT_STTS_HSTRY", r -> r.set("PET_PRTC_RCRT_SN", rcrtSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("PCAG01", before)).set("AFTR_STTS_CD", c.of("PCAG01", after))
                .set("REG_DT", at));
    }

    /**
     * 채팅방·참여자·메시지
     * @return 채팅방 번호
     */
    private long chatRoom(Seed s, long pstSn, World.Member author, World.Member applicant, LocalDateTime at, String matchStatus,
            boolean confirmed, boolean closed) {
        boolean readOnly = closed && !confirmed;
        Row room = s.store().add("OTO_CHAT_ROOM", r -> r.set("PST_SN", pstSn).set("CTPT_MBR_SN", applicant.sn())
                .set("CHAT_ROOM_STTS_CD", c.of("CHTG01", readOnly ? "읽기 전용" : "활성")).set("REG_DT", at));
        s.store().add("OTO_CHAT_ROOM_STTS_HSTRY", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MBR_SN", applicant.sn())
                .set("AFTR_STTS_CD", c.of("CHTG01", "활성")).set("REG_DT", at));
        if (readOnly) {
            s.store().add("OTO_CHAT_ROOM_STTS_HSTRY", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("BFR_STTS_CD", c.of("CHTG01", "활성"))
                    .set("AFTR_STTS_CD", c.of("CHTG01", "읽기 전용")).set("CHG_RSN", "다른 지원자와 거래 완료").set("REG_DT", s.after(at)));
        }

        int count = s.between(6, 20);
        LocalDateTime t = at;
        LocalDateTime last = at;
        for (int seq = 1; seq <= count; seq++) {
            boolean fromAuthor = seq % 2 == 0;
            long sender = fromAuthor ? author.sn() : applicant.sn();
            String type = seq == count && matchStatus != null ? "거래 카드" : s.weighted(List.of("텍스트", "사진", "주소"), 88, 9, 3);
            int msgSeq = seq;
            LocalDateTime when = t;
            Row msg = s.store().add("CHAT_MSG", r -> {
                r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MBR_SN", sender).set("MSG_TYPE_CD", c.of("CHTG03", type))
                        .set("MSG_SEQ", msgSeq).set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", when);
                switch (type) {
                case "텍스트" -> r.set("MSG_CN_ENC", crypto.encrypt(Fake.CHAT_LINES.get((msgSeq - 1) % Fake.CHAT_LINES.size())));
                case "주소" -> r.set("SHRN_ADDR_ENC", crypto.encrypt("행복로 " + s.between(1, 99) + " 앞"));
                case "거래 카드" -> r.set("MSG_CN_ENC", crypto.encrypt("돌봄 조건을 보냈어요"));
                default -> {
                }
                }
            });
            if ("사진".equals(type)) {
                for (int k = 1, n = s.between(1, 2); k <= n; k++) {
                    long fileSn = s.images().add(SeedImages.Kind.CHAT);
                    int sort = k;
                    s.store().add("CHAT_MSG_PHOTO", r -> r.set("CHAT_MSG_SN", msg.sn()).set("ATCH_FILE_SN", fileSn).set("SORT_SEQ", sort)
                            .set("REG_DT", when));
                }
            }
            last = when;
            t = s.soonAfter(t, 90);
        }
        if (confirmed) {
            int seq = count + 1;
            LocalDateTime when = s.soonAfter(last, 10);
            s.store().add("CHAT_MSG", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MSG_TYPE_CD", c.of("CHTG03", "시스템"))
                    .set("MSG_SEQ", seq).set("MSG_CN_ENC", crypto.encrypt("매칭이 완료되었어요")).set("IDMPT_DMND_ID", s.uuid())
                    .set("REG_DT", when));
            count = seq;
            last = when;
        }
        boolean left = !confirmed && s.chance(0.1);
        participant(s, room.sn(), author.sn(), s.chance(0.7) ? count : Math.max(0, count - 2), last, false, at);
        participant(s, room.sn(), applicant.sn(), count, last, left, at);
        return room.sn();
    }

    private void participant(Seed s, long roomSn, long mbrSn, int readSeq, LocalDateTime readAt, boolean left, LocalDateTime at) {
        Row p = s.store().add("CHAT_PRTPNT", r -> r.set("OTO_CHAT_ROOM_SN", roomSn).set("MBR_SN", mbrSn)
                .set("PTCP_STTS_CD", c.of("CHTG02", left ? "나감" : "참여 중")).set("LAST_PRSL_MSG_SEQ", readSeq)
                .set("LAST_PRSL_DT", readSeq > 0 ? readAt : null).set("REG_DT", at));
        s.store().add("CHAT_PRTPNT_STTS_HSTRY", r -> r.set("CHAT_PRTPNT_SN", p.sn()).set("MBR_SN", mbrSn)
                .set("AFTR_STTS_CD", c.of("CHTG02", "참여 중")).set("REG_DT", at));
        if (left) {
            s.store().add("CHAT_PRTPNT_STTS_HSTRY", r -> r.set("CHAT_PRTPNT_SN", p.sn()).set("MBR_SN", mbrSn)
                    .set("BFR_STTS_CD", c.of("CHTG02", "참여 중")).set("AFTR_STTS_CD", c.of("CHTG02", "나감")).set("REG_DT", readAt));
        }
    }

    /**
     * 매칭과 조건 버전·이력·돌봄 완료 요청
     */
    private Row match(Seed s, Row rcrt, long roomSn, World.Member author, World.Member sitter, String status, LocalDateTime chatAt) {
        List<String> path = MATCH_PATHS.get(status);
        LocalDateTime requestedAt = s.soonAfter(chatAt, 60 * 24);
        boolean done = "거래 완료".equals(status);
        Row match = s.store().add("PET_PRTC_MTCH", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("OTO_CHAT_ROOM_SN", roomSn)
                .set("MTCH_DLNG_STTS_CD", c.of("PCAG05", status)).set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", requestedAt));
        int versions = s.chance(0.3) ? 2 : 1;
        Row version = null;
        LocalDateTime t = requestedAt;
        for (int v = 1; v <= versions; v++) {
            int no = v;
            LocalDateTime when = t;
            LocalDateTime begin = rcrt.get("PRTC_BGNG_DT") == null ? when.plusDays(s.between(1, 5)).withHour(10).withMinute(0)
                    : (LocalDateTime) rcrt.get("PRTC_BGNG_DT");
            version = s.store().add("MTCH_CND_VER", r -> r.set("PET_PRTC_MTCH_SN", match.sn()).set("MBR_SN", author.sn())
                    .set("CST_UNIT_CD", rcrt.get("CST_UNIT_CD")).set("CND_VER_NO", no)
                    .set("ACRD_CST_AMT", ((Number) rcrt.get("CST_AMT")).intValue() + (no - 1) * 5_000)
                    .set("ACRD_PRTC_CN", "산책 1회, 밥 주기, 배변 정리" + (no > 1 ? ", 놀이 30분 추가" : "")).set("ACRD_PRTC_BGNG_DT", begin)
                    .set("ACRD_PRTC_END_DT", begin.plusHours(s.between(2, 48))).set("REG_DT", when));
            t = s.soonAfter(t, 60 * 12);
        }
        Row finalVersion = version;
        String before = null;
        LocalDateTime stepAt = requestedAt;
        for (String step : path) {
            String bfr = before;
            LocalDateTime when = stepAt;
            long actor = List.of("수락 대기", "확인 대기").contains(step) ? sitter.sn() : author.sn();
            s.store().add("PET_PRTC_MTCH_STTS_HSTRY", r -> r.set("PET_PRTC_MTCH_SN", match.sn())
                    .set("MBR_SN", "무응답 종료".equals(step) ? null : actor)
                    .set("MTCH_CND_VER_SN", "매칭 완료".equals(step) ? finalVersion.sn() : null)
                    .set("BFR_STTS_CD", bfr == null ? null : c.of("PCAG05", bfr)).set("AFTR_STTS_CD", c.of("PCAG05", step))
                    .set("REG_DT", when));
            before = step;
            stepAt = s.soonAfter(stepAt, 60 * 24);
        }
        if (path.contains("확인 대기")) {
            completionRequests(s, match.sn(), finalVersion, sitter, status);
        }
        if (done) {
            match.yn("RCV_YN", true).set("RCV_IDNTY_DT", stepAt);
        }
        return match;
    }

    private void completionRequests(Seed s, long matchSn, Row version, World.Member sitter, String status) {
        boolean confirmedByGuardian = List.of("수령 대기", "거래 완료").contains(status);
        int cycles = s.chance(0.2) ? 2 : 1;
        LocalDateTime endAt = (LocalDateTime) version.get("ACRD_PRTC_END_DT");
        LocalDateTime t = endAt.isAfter(s.now()) ? s.now().minusHours(1) : endAt;
        for (int cycle = 1; cycle <= cycles; cycle++) {
            boolean lastCycle = cycle == cycles;
            String result = !lastCycle ? "보완 요청" : confirmedByGuardian ? "완료 확인" : null;
            int no = cycle;
            LocalDateTime when = t;
            Row req = s.store().add("PRTC_CMPTN_DMND_CYCL", r -> r.set("PET_PRTC_MTCH_SN", matchSn).set("MTCH_CND_VER_SN", version.sn())
                    .set("MBR_SN", sitter.sn()).set("PRTCR_IDNTY_RSLT_CD", result == null ? null : c.of("PCAG06", result))
                    .set("DMND_CYCL", no).set("CMPTN_CN", "산책 40분, 식사 완료, 배변 상태 정상이에요.").set("IDMPT_DMND_ID", s.uuid())
                    .set("PRTC_END_DT", when).set("SPLMNT_DMND_RSN", "보완 요청".equals(result) ? "산책 사진도 보내 주세요" : null)
                    .set("PRTCR_IDNTY_DT", result == null ? null : s.soonAfter(when, 120))
                    .yn("HNOV_CMPTN_IDNTY_YN", "거래 완료".equals(status)).set("SPCL_MTTR_CN", s.chance(0.3) ? "물을 평소보다 많이 마셨어요" : null)
                    .set("REG_DT", when));
            for (int k = 0, n = s.between(1, 3); k < n; k++) {
                long fileSn = s.images().add(SeedImages.Kind.CARE);
                int sort = (k + 1) * 10;
                s.store().add("PRTC_CMPTN_PHOTO", r -> r.set("PRTC_CMPTN_DMND_CYCL_SN", req.sn()).set("ATCH_FILE_SN", fileSn)
                        .set("SORT_SEQ", sort).set("REG_DT", when));
            }
            t = s.soonAfter(t, 180);
        }
    }

    // ---- 좋아요·댓글·신고

    private void likes(Seed s, World w, List<World.Member> people) {
        List<Row> posts = s.store().rows("PST");
        Set<String> used = new HashSet<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (int made = 0, tries = 0; made < s.scaled(5000) && tries < s.scaled(20000); tries++) {
            Row post = s.pick(posts);
            World.Member m = s.pick(people);
            if (!used.add(post.sn() + ":" + m.sn())) {
                continue;
            }
            made++;
            boolean active = !s.chance(0.05);
            LocalDateTime at = s.after(later((LocalDateTime) post.get("REG_DT"), m.joinedAt()));
            s.store().add("PST_RCMDTN", r -> r.set("PST_SN", post.sn()).set("MBR_SN", m.sn()).yn("USE_YN", active).set("REG_DT", at));
            if (active) {
                counts.merge(post.sn(), 1, Integer::sum);
            }
        }
        counts.forEach((pstSn, n) -> s.store().row("PST", pstSn).set("RCMDTN_CNT", n));
    }

    private void comments(Seed s, World w, List<World.Member> people) {
        List<Row> posts = s.store().rows("PST");
        for (int i = 0; i < s.scaled(4000); i++) {
            Row post = s.pick(posts);
            World.Member m = s.pick(people);
            List<Long> tops = topComments.get(post.sn());
            Long parent = tops != null && s.chance(0.25) ? s.pick(tops) : null;
            LocalDateTime at = s.after(later((LocalDateTime) post.get("REG_DT"), m.joinedAt()));
            Row cmnt = s.store().add("CMNT", r -> r.set("PRNT_CMNT_SN", parent).set("PST_SN", post.sn()).set("MBR_SN", m.sn())
                    .set("CMNT_CN", s.pick(Fake.COMMENTS)).yn("DEL_YN", s.chance(0.03)).set("REG_DT", at));
            if (parent == null) {
                topComments.computeIfAbsent(post.sn(), k -> new ArrayList<>()).add(cmnt.sn());
            }
        }
    }

    private void reports(Seed s, World w, List<World.Member> people) {
        List<World.Member> admins = w.admins();
        Set<String> used = new HashSet<>();
        for (Row post : blindPosts) {
            for (World.Member m : s.sample(people.stream().filter(p -> p.sn() != (Long) post.get("MBR_SN")).toList(), BLIND_REPORTS)) {
                used.add(m.sn() + ":P" + post.sn());
                report(s, m, post.sn(), null, "처리완료", s.pick(admins), (LocalDateTime) post.get("REG_DT"));
            }
        }
        List<Row> posts = s.store().rows("PST");
        List<Row> cmnts = s.store().rows("CMNT");
        for (int made = 0, tries = 0; made < s.scaled(35) && tries < s.scaled(1000); tries++) {
            World.Member m = s.pick(people);
            boolean onPost = s.chance(0.7);
            Row target = onPost ? s.pick(posts) : s.pick(cmnts);
            if (!used.add(m.sn() + (onPost ? ":P" : ":C") + target.sn()) || m.sn() == (Long) target.get("MBR_SN")) {
                continue;
            }
            made++;
            String status = s.weighted(List.of("접수", "처리완료", "반려"), 60, 25, 15);
            report(s, m, onPost ? target.sn() : null, onPost ? null : target.sn(), status, s.pick(admins),
                    (LocalDateTime) target.get("REG_DT"));
        }
    }

    private void report(Seed s, World.Member m, Long pstSn, Long cmntSn, String status, World.Member admin, LocalDateTime targetAt) {
        LocalDateTime at = s.after(later(targetAt, m.joinedAt()));
        boolean processed = !"접수".equals(status);
        LocalDateTime processedAt = processed ? s.after(at) : null;
        String reason = s.pick(c.all("CMUG04"));
        Row dclr = s.store().add("DCLR", r -> r.set("CMNT_SN", cmntSn).set("MBR_SN", m.sn()).set("PRCS_MBR_SN", processed ? admin.sn() : null)
                .set("PST_SN", pstSn).set("DCLR_RSN_CD", reason).set("DCLR_PRCS_STTS_CD", c.of("CMUG05", status))
                .set("DCLR_DTL_CN", s.chance(0.5) ? "확인 부탁드립니다." : null).set("PRCS_DT", processedAt).set("REG_DT", at));
        s.store().add("DCLR_STTS_HSTRY", r -> r.set("DCLR_SN", dclr.sn()).set("MBR_SN", m.sn())
                .set("AFTR_STTS_CD", c.of("CMUG05", "접수")).set("REG_DT", at));
        if (processed) {
            s.store().add("DCLR_STTS_HSTRY", r -> r.set("DCLR_SN", dclr.sn()).set("MBR_SN", admin.sn())
                    .set("BFR_STTS_CD", c.of("CMUG05", "접수")).set("AFTR_STTS_CD", c.of("CMUG05", status))
                    .set("CHG_RSN", "처리완료".equals(status) ? "운영정책 위반 확인" : "위반 사항 없음").set("REG_DT", processedAt));
        }
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
