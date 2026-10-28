package com.nyangmeong.seed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.MemberStatus;

/**
 * [반려동물·양육관계]
 * - 회원마다 0~10마리(1번 3마리, 3번 0마리, 4번 10마리). 합계는 배율 × 1,000
 * - PET, BRUP_REL, BRUP_LINK_STTS_HSTRY, JNT_BRUP_INVTN, INVTN_TRGT_PET, JNT_BRUP_INVTN_STTS_HSTRY
 */
public class PetSeeder implements Seeder {

    private static final int PET_MAX = 10;
    private static final int CAREGIVER_MAX = 3;

    private static final Set<String> SMALL = Set.of("말티즈", "푸들", "포메라니안", "비숑 프리제", "시츄", "치와와", "요크셔 테리어", "말티푸",
            "페키니즈", "닥스훈트");
    private static final Set<String> LARGE = Set.of("보더 콜리", "사모예드", "시베리안 허스키", "골든 리트리버", "래브라도 리트리버", "진돗개");
    private static final List<String> DOG_COLORS = List.of("검정", "흰색", "갈색", "크림", "회색", "검정&황갈색", "얼룩", "브린들");
    private static final List<String> CAT_COLORS = List.of("검정", "흰색", "회색", "삼색", "고등어", "치즈", "턱시도", "얼룩", "크림");
    private static final List<String> TRAITS = List.of("겁이 많아요", "사람을 좋아해요", "산책을 좋아해요", "간식에 진심이에요", "낯을 가려요",
            "애교가 많아요", "잠이 많아요", "호기심이 많아요");
    private static final List<String> ALLERGIES = List.of("닭고기 알레르기", "소고기 알레르기", "곡물 알레르기", "연어 알레르기");
    private static final List<String> DISEASES = List.of("슬개골 탈구 1기", "피부염", "심장 판막 질환 관찰 중", "신장 수치 관찰 중", "치주염");

    /** 회원 → 양육관계(활성) 수. 주양육·공동양육 합 */
    private final Map<Long, Integer> relCount = new HashMap<>();
    /** 반려동물 → 활성 양육자 회원 */
    private final Map<Long, Set<Long>> caregivers = new HashMap<>();
    /** 회원 → 활성 양육관계 행 */
    private final Map<Long, List<Row>> activeRels = new LinkedHashMap<>();

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        Map<Long, Integer> plan = planCounts(s, w);
        for (Map.Entry<Long, Integer> e : plan.entrySet()) {
            World.Member owner = w.member(e.getKey());
            Set<String> names = new HashSet<>();
            for (int i = 0; i < e.getValue(); i++) {
                World.Pet pet = pet(s, w, c, owner, names);
                ownerRel(s, c, pet);
            }
        }
        joint(s, w, c);
        invitations(s, w, c);
        representatives(s, w);
        w.caredPets.putAll(worldCare);
    }

    /**
     * 회원별 마릿수
     * - 탈퇴·관리자·3번은 0, 1번 3, 4번 10. 나머지를 0~10으로 나눈 뒤 합계를 맞춘다
     */
    private Map<Long, Integer> planCounts(Seed s, World w) {
        Map<Long, Integer> plan = new LinkedHashMap<>();
        List<Long> flexible = new ArrayList<>();
        for (World.Member m : w.members) {
            if (m.admin() || m.status() == MemberStatus.WITHDRAWN || m.sn() == 3) {
                continue;
            }
            if (m.sn() == 1) {
                plan.put(m.sn(), 3);
            } else if (m.sn() == 4) {
                plan.put(m.sn(), PET_MAX);
            } else {
                plan.put(m.sn(), s.between(0, PET_MAX));
                flexible.add(m.sn());
            }
        }
        int target = s.scaled(1000);
        int sum = plan.values().stream().mapToInt(Integer::intValue).sum();
        while (sum != target) {
            long sn = s.pick(flexible);
            int n = plan.get(sn);
            // 꽉 찬 회원도 공동양육 여유를 두려고 9마리까지만 늘린다
            if (sum < target && n < PET_MAX - 1) {
                plan.put(sn, n + 1);
                sum++;
            } else if (sum > target && n > 0) {
                plan.put(sn, n - 1);
                sum--;
            }
        }
        return plan;
    }

    private World.Pet pet(Seed s, World w, Codes c, World.Member owner, Set<String> usedNames) {
        boolean dog = s.chance(0.6);
        List<Long> varieties = dog ? w.dogVarieties : w.catVarieties;
        Long vrtySn = s.chance(0.9) ? s.pick(varieties) : null;
        String breed = vrtySn == null ? null : w.varietyNames.get(vrtySn);
        double weight = weightOf(s, dog, breed);
        String name = petName(s, usedNames);
        boolean deleted = owner.sn() > 5 && s.chance(0.03);
        LocalDate birth = s.today().minusDays(s.between(90, 365 * 15));
        // 가입 직후 등록이 많고, 나중에 더 들인 반려동물도 있다
        LocalDateTime at = s.chance(0.6) ? s.soonAfter(owner.joinedAt(), 60 * 24 * 3) : s.after(owner.joinedAt());
        String color = s.pick(dog ? DOG_COLORS : CAT_COLORS);
        boolean customColor = s.chance(0.05);
        boolean meal = s.chance(0.7);

        Row row = s.store().add("PET", r -> {
            r.set("VRTY_SN", vrtySn).set("PET_SE_CD", c.of("PETG01", dog ? "강아지" : "고양이"))
                    .set("PET_GNDR_CD", c.of("PETG02", s.pick("수컷", "암컷")))
                    .set("COLR_CD", c.of("PETG03", customColor ? "직접 입력" : color)).set("PET_NM", name)
                    .set("PET_BRDT", birth).set("PET_BDWG", s.decimal(weight, 3))
                    .set("MIN_BDWG", s.decimal(weight * 0.9, 3)).set("MAX_BDWG", s.decimal(weight * 1.1, 3))
                    .yn("NUTR_YN", s.chance(0.7)).yn("DEL_YN", deleted).set("REG_DT", at);
            if (vrtySn == null) {
                r.set("VRTY_DRCT_INPT_NM", dog ? s.pick("믹스견", "진도 믹스", "푸들 믹스") : s.pick("믹스묘", "코숏 믹스"));
            }
            if (customColor) {
                r.set("COLR_DRCT_INPT_NM", s.pick("은회색", "초코 브라운", "블루 그레이"));
            }
            if (dog && s.chance(0.6)) {
                r.set("CHIP_NO", String.format("410%03d%09d", s.between(0, 999), s.between(0, 999_999_999)));
            }
            if (s.chance(0.3)) {
                r.set("GOAL_BDWG", s.decimal(weight * s.between(0.9, 1.0), 3));
            }
            if (meal) {
                double food = weight * (dog ? 20 : 15);
                r.set("MIN_MEAL_QTY", s.decimal(food * 0.9, 3)).set("MAX_MEAL_QTY", s.decimal(food * 1.1, 3))
                        .set("MIN_WATR_QTY", s.decimal(weight * 40, 3)).set("MAX_WATR_QTY", s.decimal(weight * 60, 3));
            }
            if (s.chance(0.5)) {
                r.set("CHRC_CN", s.pick(TRAITS));
            }
            if (s.chance(0.15)) {
                r.set("ALGY_CN", s.pick(ALLERGIES));
            }
            if (s.chance(0.15)) {
                r.set("DSS_CN", s.pick(DISEASES));
            }
        });
        World.Pet pet = new World.Pet(row.sn(), owner.sn(), dog, name, deleted, vrtySn, birth,
                BigDecimal.valueOf(weight), at);
        w.pets.add(pet);
        return pet;
    }

    private double weightOf(Seed s, boolean dog, String breed) {
        if (!dog) {
            return "메인쿤".equals(breed) ? s.between(5.0, 9.0) : s.between(3.0, 6.5);
        }
        if (breed != null && SMALL.contains(breed)) {
            return s.between(2.0, 7.0);
        }
        if (breed != null && LARGE.contains(breed)) {
            return s.between(16.0, 34.0);
        }
        return s.between(6.0, 18.0);
    }

    private String petName(Seed s, Set<String> used) {
        String name = s.pick(Fake.PET_NAMES);
        for (int n = 2; used.contains(name); n++) {
            name = s.pick(Fake.PET_NAMES) + (n > 5 ? String.valueOf(n) : "");
        }
        used.add(name);
        return name;
    }

    private void ownerRel(Seed s, Codes c, World.Pet pet) {
        Row rel = s.store().add("BRUP_REL", r -> r.set("PET_SN", pet.sn()).set("MBR_SN", pet.ownerSn())
                .set("BRUP_ROLE_CD", c.of("PETG05", "주양육자")).yn("USE_YN", !pet.deleted()).set("REG_DT", pet.registeredAt()));
        if (pet.deleted()) {
            LocalDateTime at = s.after(pet.registeredAt());
            linkHistory(s, c, rel.sn(), pet.ownerSn(), "반려동물 삭제 해제", "반려동물 삭제", at);
            return;
        }
        track(rel, pet.ownerSn(), pet.sn());
    }

    /**
     * 공동양육
     * - 1번 회원이 남의 반려동물 1마리를 공동양육하고, 1번의 반려동물에도 공동양육자가 있게 먼저 만든다
     */
    private void joint(Seed s, World w, Codes c) {
        List<World.Pet> live = new ArrayList<>(w.livePets());
        List<World.Member> people = w.members.stream().filter(m -> m.status() == MemberStatus.ACTIVE && !m.admin()
                && m.sn() != 3).toList();
        World.Pet othersPet = live.stream().filter(p -> p.ownerSn() != 1 && p.ownerSn() != 4).findFirst().orElseThrow();
        addJoint(s, w, c, othersPet, w.member(1));
        World.Pet firstPet = live.stream().filter(p -> p.ownerSn() == 1).findFirst().orElseThrow();
        addJoint(s, w, c, firstPet, people.stream().filter(m -> m.sn() > 5).findFirst().orElseThrow());

        int target = s.scaled(150);
        int made = 2;
        for (int tries = 0; made < target && tries < target * 20; tries++) {
            World.Pet pet = s.pick(live);
            World.Member member = s.pick(people);
            if (addJoint(s, w, c, pet, member)) {
                made++;
            }
        }
        // 끝난 공동양육(자진·강제 해제)
        for (int i = 0; i < s.scaled(20); i++) {
            World.Pet pet = s.pick(live);
            World.Member member = s.pick(people);
            if (member.sn() == pet.ownerSn() || caregivers.get(pet.sn()).contains(member.sn())
                    || endedPairs.contains(pet.sn() + ":" + member.sn())) {
                continue;
            }
            endedPairs.add(pet.sn() + ":" + member.sn());
            LocalDateTime at = s.after(later(pet.registeredAt(), member.joinedAt()));
            Row rel = s.store().add("BRUP_REL", r -> r.set("PET_SN", pet.sn()).set("MBR_SN", member.sn())
                    .set("BRUP_ROLE_CD", c.of("PETG05", "공동양육자")).yn("USE_YN", false).set("REG_DT", at));
            linkHistory(s, c, rel.sn(), member.sn(), "초대 수락 연동", null, at);
            boolean self = s.chance(0.6);
            linkHistory(s, c, rel.sn(), self ? member.sn() : pet.ownerSn(), self ? "자진 해제" : "강제 해제",
                    self ? "이사로 함께 돌보기 어려워졌어요" : "주양육자가 연동을 해제했어요", s.after(at));
        }
    }

    private final Set<String> endedPairs = new HashSet<>();

    private boolean addJoint(Seed s, World w, Codes c, World.Pet pet, World.Member member) {
        if (member.sn() == pet.ownerSn() || caregivers.get(pet.sn()).contains(member.sn())
                || caregivers.get(pet.sn()).size() >= CAREGIVER_MAX || relCount.getOrDefault(member.sn(), 0) >= PET_MAX) {
            return false;
        }
        LocalDateTime sentAt = s.after(later(pet.registeredAt(), member.joinedAt()));
        LocalDateTime acceptedAt = s.soonAfter(sentAt, 60 * 24 * 3);
        FieldCryptoService crypto = s.crypto();
        Row invitation = s.store().add("JNT_BRUP_INVTN", r -> r.set("SNDR_MBR_SN", pet.ownerSn())
                .set("RCVR_MBR_SN", member.sn()).set("INVTN_STTS_CD", c.of("PETG06", "수락"))
                .set("INVTN_TRGT_EML_ENC", crypto.encrypt(member.email())).set("INVTN_TRGT_EML_HMAC", crypto.emailHmac(member.email()))
                .set("INVTN_EXPRY_DT", sentAt.plusDays(7)).set("ACPT_RFSL_RSPNS_DT", acceptedAt)
                .set("INVTN_TKN_HASH", s.hex64()).set("REG_DT", sentAt));
        s.store().add("INVTN_TRGT_PET", r -> r.set("JNT_BRUP_INVTN_SN", invitation.sn()).set("PET_SN", pet.sn()).set("REG_DT", sentAt));
        invitationHistory(s, c, invitation.sn(), pet.ownerSn(), null, "대기중", sentAt);
        invitationHistory(s, c, invitation.sn(), member.sn(), "대기중", "수락", acceptedAt);

        Row rel = s.store().add("BRUP_REL", r -> r.set("PET_SN", pet.sn()).set("MBR_SN", member.sn())
                .set("BRUP_ROLE_CD", c.of("PETG05", "공동양육자")).set("REG_DT", acceptedAt));
        linkHistory(s, c, rel.sn(), member.sn(), "초대 수락 연동", null, acceptedAt);
        track(rel, member.sn(), pet.sn());
        return true;
    }

    /**
     * 끝나지 않았거나 거절·만료된 초대
     */
    private void invitations(Seed s, World w, Codes c) {
        FieldCryptoService crypto = s.crypto();
        List<World.Pet> live = w.livePets();
        for (int i = 0; i < s.scaled(50); i++) {
            World.Pet pet = s.pick(live);
            World.Member sender = w.member(pet.ownerSn());
            String result = s.weighted(List.of("대기중", "거절", "만료됨"), 4, 3, 3);
            boolean toMember = !"대기중".equals(result) || s.chance(0.6);
            World.Member receiver = toMember ? s.pick(w.activeMembers()) : null;
            String email = receiver != null && receiver.sn() != sender.sn() ? receiver.email()
                    : String.format("friend%04d@example.com", i);
            Long receiverSn = receiver != null && receiver.sn() != sender.sn() ? receiver.sn() : null;
            LocalDateTime sentAt = "대기중".equals(result) ? s.daysAgo(0, 5) : s.after(pet.registeredAt());
            LocalDateTime respondedAt = "거절".equals(result) ? s.soonAfter(sentAt, 60 * 24 * 5) : null;
            Row invitation = s.store().add("JNT_BRUP_INVTN", r -> r.set("SNDR_MBR_SN", sender.sn()).set("RCVR_MBR_SN",
                    "거절".equals(result) ? receiverSn : null)
                    .set("INVTN_STTS_CD", c.of("PETG06", result)).set("INVTN_TRGT_EML_ENC", crypto.encrypt(email))
                    .set("INVTN_TRGT_EML_HMAC", crypto.emailHmac(email)).set("INVTN_EXPRY_DT", sentAt.plusDays(7))
                    .set("ACPT_RFSL_RSPNS_DT", respondedAt).set("INVTN_TKN_HASH", s.hex64()).set("REG_DT", sentAt));
            List<Long> senderPets = live.stream().filter(p -> p.ownerSn() == sender.sn()).map(World.Pet::sn).toList();
            for (Long petSn : s.sample(senderPets, s.between(1, 2))) {
                s.store().add("INVTN_TRGT_PET", r -> r.set("JNT_BRUP_INVTN_SN", invitation.sn()).set("PET_SN", petSn)
                        .set("REG_DT", sentAt));
            }
            invitationHistory(s, c, invitation.sn(), sender.sn(), null, "대기중", sentAt);
            if ("거절".equals(result)) {
                invitationHistory(s, c, invitation.sn(), receiverSn, "대기중", "거절", respondedAt);
            } else if ("만료됨".equals(result)) {
                invitationHistory(s, c, invitation.sn(), null, "대기중", "만료됨", sentAt.plusDays(7));
            }
        }
    }

    /**
     * 대표 반려동물: 활성 관계가 있는 회원마다 정확히 1마리(주양육 첫 마리 우선)
     */
    private void representatives(Seed s, World w) {
        activeRels.forEach((mbrSn, rels) -> {
            Row chosen = rels.stream().filter(r -> w.pet((Long) r.get("PET_SN")).ownerSn() == mbrSn).findFirst()
                    .orElse(rels.get(0));
            chosen.yn("RPRS_PET_YN", true);
            w.representativePet.put(mbrSn, (Long) chosen.get("PET_SN"));
        });
    }

    private void track(Row rel, long mbrSn, long petSn) {
        relCount.merge(mbrSn, 1, Integer::sum);
        caregivers.computeIfAbsent(petSn, k -> new HashSet<>()).add(mbrSn);
        activeRels.computeIfAbsent(mbrSn, k -> new ArrayList<>()).add(rel);
        worldCare.computeIfAbsent(mbrSn, k -> new ArrayList<>()).add(petSn);
    }

    /** track이 채우고 run 끝에 World로 넘긴다 */
    private final Map<Long, List<Long>> worldCare = new HashMap<>();

    private void linkHistory(Seed s, Codes c, long relSn, long mbrSn, String type, String reason, LocalDateTime at) {
        s.store().add("BRUP_LINK_STTS_HSTRY", r -> r.set("BRUP_REL_SN", relSn).set("MBR_SN", mbrSn)
                .set("LINK_CHG_TYPE_CD", c.of("PETG07", type)).set("CHG_RSN", reason).set("REG_DT", at));
    }

    private void invitationHistory(Seed s, Codes c, long invitationSn, Long mbrSn, String before, String after,
            LocalDateTime at) {
        s.store().add("JNT_BRUP_INVTN_STTS_HSTRY", r -> r.set("JNT_BRUP_INVTN_SN", invitationSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("PETG06", before)).set("AFTR_STTS_CD", c.of("PETG06", after))
                .set("REG_DT", at));
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
