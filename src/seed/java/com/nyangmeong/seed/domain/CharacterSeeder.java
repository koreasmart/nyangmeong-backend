package com.nyangmeong.seed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [픽셀 캐릭터·아이템·케어미션·포인트]
 * - PIXEL_CHAR 배율 × 600(대표 반려동물 먼저), 생성 작업·사진·이력, 보유 아이템
 * - DAY_MSN 대표 반려동물 최근 14일, PNT_GNLGR 가입 포인트 + 미션 지급 + 아이템 구매. MBR.HLD_PNT = 마지막 잔액
 */
public class CharacterSeeder implements Seeder {

    private static final int WELCOME_POINT = 5000;

    private record PointEvent(LocalDateTime at, long mbrSn, int amount, String type, Long dayMsnSn, Long hldItemSn,
            String memo) {
    }

    private final List<PointEvent> events = new ArrayList<>();

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        characters(s, w, c);
        missions(s, w, c);
        ledger(s, w, c);
    }

    private void characters(Seed s, World w, Codes c) {
        Set<Long> petSns = new LinkedHashSet<>();
        w.caredPets.getOrDefault(1L, List.of()).forEach(petSns::add);
        List<Long> reps = new ArrayList<>(w.representativePet.values());
        s.shuffle(reps);
        petSns.addAll(reps);
        List<Long> others = new ArrayList<>(w.livePets().stream().map(World.Pet::sn).toList());
        s.shuffle(others);
        petSns.addAll(others);
        List<Row> items = s.store().rows("ITEM");
        int target = s.scaled(600);
        for (long petSn : petSns) {
            if (s.store().rows("PIXEL_CHAR").size() >= target) {
                break;
            }
            World.Pet pet = w.pet(petSn);
            character(s, w, c, pet, items);
        }
    }

    private void character(Seed s, World w, Codes c, World.Pet pet, List<Row> items) {
        LocalDateTime createdAt = s.after(pet.registeredAt());
        Row ch = s.store().add("PIXEL_CHAR", r -> r.set("MBR_SN", pet.ownerSn()).set("PET_SN", pet.sn()).set("REG_DT", createdAt));
        w.characterPet.put(ch.sn(), pet.sn());
        int jobs = s.chance(0.25) ? 2 : 1;
        LocalDateTime at = createdAt;
        Row currentPhoto = null;
        for (int j = 0; j < jobs; j++) {
            boolean last = j == jobs - 1;
            String status = last ? s.weighted(List.of("생성 완료", "생성 실패", "생성 중", "요청"), 80, 10, 5, 5) : "생성 완료";
            if (!last || !List.of("생성 중", "요청").contains(status)) {
                at = j == 0 ? at : s.after(at);
            } else {
                at = s.now().minusMinutes(s.between(1, 20));
            }
            Row photo = job(s, c, pet, ch.sn(), status, at);
            if (photo != null) {
                currentPhoto = photo;
            }
        }
        if (currentPhoto == null) {
            ch.yn("BSC_CHAR_SBST_YN", true);
            return;
        }
        currentPhoto.yn("NOW_INDCT_YN", true);
        w.characterPhoto.put(ch.sn(), currentPhoto.sn());
        heldItems(s, ch, pet, items, createdAt);
    }

    /**
     * 생성 작업 하나
     * @return 완료면 결과 사진 행, 아니면 null
     */
    private Row job(Seed s, Codes c, World.Pet pet, long charSn, String status, LocalDateTime at) {
        boolean running = List.of("생성 중", "요청").contains(status);
        LocalDateTime began = s.soonAfter(at, 2);
        LocalDateTime ended = running ? null : s.soonAfter(began, 5);
        Row job = s.store().add("CHAR_CRT_JOB", r -> r.set("PET_SN", pet.sn()).set("MBR_SN", pet.ownerSn())
                .set("CRT_JOB_STTS_CD", c.of("CHRG01", status)).set("PRCS_BGNG_DT", "요청".equals(status) ? null : began)
                .set("PRCS_END_DT", ended).set("FAIL_RSN", "생성 실패".equals(status) ? "사진에서 반려동물을 찾지 못했어요" : null)
                .set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", at));
        List<String> path = switch (status) {
        case "요청" -> List.of("요청");
        case "생성 중" -> List.of("요청", "생성 중");
        default -> List.of("요청", "생성 중", status);
        };
        String before = null;
        LocalDateTime t = at;
        for (String step : path) {
            String bfr = before;
            LocalDateTime when = t;
            s.store().add("CHAR_CRT_JOB_STTS_HSTRY", r -> r.set("CHAR_CRT_JOB_SN", job.sn()).set("MBR_SN", pet.ownerSn())
                    .set("BFR_STTS_CD", bfr == null ? null : c.of("CHRG01", bfr)).set("AFTR_STTS_CD", c.of("CHRG01", step))
                    .set("REG_DT", when));
            before = step;
            t = "요청".equals(step) ? began : ended == null ? t : ended;
        }
        long originalFile = s.images().addPetPhoto(pet.dog());
        s.store().add("CHAR_PHOTO", r -> r.set("PIXEL_CHAR_SN", charSn).set("CHAR_CRT_JOB_SN", job.sn())
                .set("ATCH_FILE_SN", originalFile).set("PHOTO_ROLE_CD", c.of("FILG02", "원본")).set("SORT_SEQ", 10).set("REG_DT", at));
        if (!"생성 완료".equals(status)) {
            return null;
        }
        long resultFile = s.images().add(SeedImages.Kind.CHARACTER);
        return s.store().add("CHAR_PHOTO", r -> r.set("PIXEL_CHAR_SN", charSn).set("CHAR_CRT_JOB_SN", job.sn())
                .set("ATCH_FILE_SN", resultFile).set("PHOTO_ROLE_CD", c.of("FILG02", "결과")).set("SORT_SEQ", 20).set("REG_DT", ended));
    }

    private void heldItems(Seed s, Row ch, World.Pet pet, List<Row> items, LocalDateTime createdAt) {
        Set<String> equippedRegions = new HashSet<>();
        for (Row item : s.sample(items, s.between(0, 5))) {
            String region = item.string("ITEM_REGN_CD");
            boolean deleted = s.chance(0.03);
            boolean equip = !deleted && !equippedRegions.contains(region) && s.chance(0.5);
            if (equip) {
                equippedRegions.add(region);
            }
            LocalDateTime boughtAt = s.after(createdAt);
            Row held = s.store().add("HLD_ITEM", r -> r.set("PIXEL_CHAR_SN", ch.sn()).set("ITEM_SN", item.sn())
                    .set("EQUIP_REGN_CD", region).yn("DEL_YN", deleted).yn("EQUIP_YN", equip).set("REG_DT", boughtAt));
            int price = ((Number) item.get("NEED_PNT")).intValue();
            events.add(new PointEvent(boughtAt, pet.ownerSn(), -price, "아이템 구매", null, held.sn(),
                    item.string("ITEM_NM") + " 구매"));
        }
    }

    private void missions(Seed s, World w, Codes c) {
        List<Row> missions = s.store().rows("CARE_MSN");
        // 공동양육자도 같은 반려동물을 대표로 둘 수 있다. 일별 미션은 반려동물·날짜·미션마다 하나(UK_DAY_MSN_1)
        Set<Long> seededPets = new HashSet<>();
        for (Map.Entry<Long, Long> e : w.representativePet.entrySet()) {
            World.Member m = w.member(e.getKey());
            long petSn = e.getValue();
            if (!m.active() || !seededPets.add(petSn)) {
                continue;
            }
            for (int d = 13; d >= 0; d--) {
                LocalDate day = s.today().minusDays(d);
                if (day.isBefore(w.pet(petSn).registeredAt().toLocalDate())) {
                    continue;
                }
                for (Row mission : s.sample(missions, 3)) {
                    dayMission(s, m, petSn, mission, day, d > 0);
                }
            }
        }
    }

    private void dayMission(Seed s, World.Member m, long petSn, Row mission, LocalDate day, boolean past) {
        int point = ((Number) mission.get("CMPN_PNT")).intValue();
        int times = ((Number) mission.get("ACHV_CRTR_NMTM")).intValue();
        boolean done = s.chance(m.sn() == 1 ? 0.8 : 0.6);
        LocalDateTime doneAt = day.atTime(s.between(8, 22), s.between(0, 59));
        LocalDateTime when = doneAt.isAfter(s.now()) ? s.now() : doneAt;
        Row row = s.store().add("DAY_MSN", r -> r.set("CARE_MSN_SN", mission.sn()).set("PET_SN", petSn).set("MBR_SN", m.sn())
                .set("CMPTN_MBR_SN", done ? m.sn() : null).set("MSN_CRTR_YMD", day)
                .set("PRGRS_NMTM", done ? times : s.between(0, times - 1)).set("GIVE_PNT", done ? point : 0).set("SCOR_PNT", point)
                .set("CMPTN_DT", done ? when : null).yn("CMPTN_YN", done).yn("CLCLN_CFMTN_YN", done && past)
                .set("REG_DT", day.atStartOfDay()));
        if (done && past) {
            events.add(new PointEvent(when, m.sn(), point, "케어미션 달성", row.sn(), null, mission.string("MSN_NM")));
        }
    }

    /**
     * 포인트 원장
     * - 이벤트가 있는 회원은 가입 포인트로 시작, 시간순 잔액. 마지막 잔액을 MBR.HLD_PNT에
     */
    private void ledger(Seed s, World w, Codes c) {
        Map<Long, List<PointEvent>> byMember = new HashMap<>();
        events.forEach(e -> byMember.computeIfAbsent(e.mbrSn(), k -> new ArrayList<>()).add(e));
        for (World.Member m : w.members) {
            List<PointEvent> list = byMember.get(m.sn());
            if (list == null) {
                continue;
            }
            list.add(new PointEvent(m.joinedAt(), m.sn(), WELCOME_POINT, "관리자 조정", null, null, "가입 축하 포인트"));
            list.sort(Comparator.comparing(PointEvent::at));
            int balance = 0;
            for (PointEvent e : list) {
                // 아이템을 많이 산 회원은 잔액이 음수가 되지 않게 구매 직전에 이벤트 포인트를 준다
                if (balance + e.amount() < 0) {
                    PointEvent grant = new PointEvent(e.at().minusMinutes(1), m.sn(), WELCOME_POINT, "관리자 조정", null, null,
                            "이벤트 포인트 지급");
                    balance = write(s, c, grant, balance);
                }
                balance = write(s, c, e, balance);
            }
            s.store().row("MBR", m.sn()).set("HLD_PNT", BigDecimal.valueOf(balance));
        }
    }

    private int write(Seed s, Codes c, PointEvent e, int balance) {
        int after = balance + e.amount();
        s.store().add("PNT_GNLGR", r -> r.set("MBR_SN", e.mbrSn()).set("DAY_MSN_SN", e.dayMsnSn())
                .set("HLD_ITEM_SN", e.hldItemSn()).set("PNT_DLNG_TYPE_CD", c.of("MYPG03", e.type()))
                .set("DLNG_PNT", e.amount()).set("DLNG_AFTR_BLNC_PNT", after).set("MEMO_CN", e.memo())
                .set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", e.at()));
        return after;
    }
}
