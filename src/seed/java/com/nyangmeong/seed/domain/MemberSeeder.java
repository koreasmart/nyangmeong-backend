package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.MemberStatus;

/**
 * [회원]
 * - 고정 계정 1~5번(README "시드 데이터"), 나머지는 일반회원. 정지·탈퇴 회원을 섞는다
 * - MBR, MBR_STTS_HSTRY, SNS_ACNT_LINK, MBR_TRMS_AGRE, MBR_DSHBD_WDGT
 */
public class MemberSeeder implements Seeder {

    public static final String EMAIL_DOMAIN = "@nyangmeong.dev";

    private record Fixed(String email, String nickname, boolean admin) {
    }

    /** 1 일반(데이터 골고루), 2 관리자, 3 반려동물 0마리, 4 반려동물 10마리(상한), 5 관리자 */
    static final List<Fixed> FIXED = List.of(new Fixed("tester" + EMAIL_DOMAIN, "냥멍테스터", false),
            new Fixed("admin" + EMAIL_DOMAIN, "냥멍관리자", true), new Fixed("empty" + EMAIL_DOMAIN, "빈집사", false),
            new Fixed("full" + EMAIL_DOMAIN, "열마리집사", false), new Fixed("operator" + EMAIL_DOMAIN, "냥멍운영자", true));

    private static final String[][] WIDGETS = { { "체중 그래프" }, { "식사량 그래프" }, { "음수량 그래프" }, { "산책 시간" },
            { "혈액검사 추이" }, { "토탈 케어리스트" }, { "AI 건강분석 리포트" } };

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        FieldCryptoService crypto = s.crypto();
        int total = s.scaled(200);
        Set<Integer> suspended = new HashSet<>();
        Set<Integer> withdrawn = new HashSet<>();
        List<Integer> candidates = new ArrayList<>();
        for (int i = FIXED.size() + 1; i <= total; i++) {
            candidates.add(i);
        }
        List<Integer> picked = s.sample(candidates, s.scaled(8));
        suspended.addAll(picked.subList(0, s.scaled(3)));
        withdrawn.addAll(picked.subList(s.scaled(3), picked.size()));

        Set<String> nicknames = new HashSet<>();
        FIXED.forEach(f -> nicknames.add(f.nickname()));
        String emailJoin = c.of("USRG03", "이메일");
        List<String> joinPaths = List.of(emailJoin, c.of("USRG03", "카카오"), c.of("USRG03", "네이버"), c.of("USRG03", "구글"));

        for (int i = 1; i <= total; i++) {
            Fixed fixed = i <= FIXED.size() ? FIXED.get(i - 1) : null;
            String email = fixed != null ? fixed.email() : String.format("user%04d%s", i, EMAIL_DOMAIN);
            String nickname = fixed != null ? fixed.nickname() : nickname(s, nicknames);
            boolean admin = fixed != null && fixed.admin();
            MemberStatus status = suspended.contains(i) ? MemberStatus.SUSPENDED
                    : withdrawn.contains(i) ? MemberStatus.WITHDRAWN : MemberStatus.ACTIVE;
            LocalDateTime joinedAt = fixed != null ? s.now().minusDays(200) : s.daysAgo(40, 190);
            String joinPath = fixed != null ? emailJoin : s.weighted(joinPaths, 60, 20, 12, 8);
            boolean sns = !joinPath.equals(emailJoin);
            boolean hasPhone = fixed != null || s.chance(0.8);
            String phone = Fake.phone(i);
            boolean limitSet = fixed != null && i == 1 || s.chance(0.4);
            Long profileSn = s.chance(0.3) ? s.images().add(SeedImages.Kind.PROFILE) : null;
            int index = i;

            Row m = s.store().add("MBR", r -> {
                r.set("ATCH_FILE_SN", profileSn)
                        .set("MBR_STTS_CD", c.of("USRG01", switch (status) {
                        case ACTIVE -> "정상";
                        case SUSPENDED -> "정지";
                        case WITHDRAWN -> "탈퇴";
                        }))
                        .set("MBR_AUTHRT_CD", c.of("USRG02", admin ? "관리자" : "일반회원"))
                        .set("JOIN_PATH_CD", joinPath)
                        .set("ACNT_EML_ENC", crypto.encrypt(crypto.normalizeEmail(email)))
                        .set("ACNT_EML_HMAC", crypto.emailHmac(email))
                        .set("PSWD_HASH", sns ? null : s.passwordHash())
                        .set("MBR_NCNM", nickname)
                        .yn("NCNM_STNG_CMPTN_YN", !sns || s.chance(0.9))
                        .set("LAST_LGN_DT", status == MemberStatus.ACTIVE ? s.daysAgo(0, 20) : s.daysAgo(30, 60))
                        .set("REG_DT", joinedAt);
                if (hasPhone) {
                    r.set("MBL_TELNO_ENC", crypto.encrypt(phone)).set("MBL_TELNO_HMAC", crypto.hmac(phone));
                }
                if (limitSet) {
                    r.set("MM_EXPND_LIM_AMT", s.between(2, 10) * 100_000).yn("EXPND_LIM_NOTI_RCPTN_YN", s.chance(0.6));
                }
                if (status == MemberStatus.WITHDRAWN) {
                    r.set("WHDWL_DT", s.daysAgo(1, 25));
                }
                if (index > FIXED.size() && s.chance(0.25)) {
                    r.yn("EML_NOTI_RCPTN_YN", s.chance(0.5)).yn("CMNT_NOTI_RCPTN_YN", s.chance(0.5))
                            .yn("THDY_BFHD_NOTI_YN", s.chance(0.4)).yn("D3_BFHD_NOTI_YN", s.chance(0.4))
                            .yn("D7_BFHD_NOTI_YN", s.chance(0.3));
                }
            });
            w.members.add(new World.Member(m.sn(), email, nickname, admin, status, joinedAt));

            history(s, c, m.sn(), null, "정상", null, "회원가입", joinedAt);
            if (status == MemberStatus.SUSPENDED) {
                history(s, c, m.sn(), "정상", "정지", 2L, "커뮤니티 운영정책 위반(신고 누적)", s.daysAgo(3, 30));
            } else if (status == MemberStatus.WITHDRAWN) {
                history(s, c, m.sn(), "정상", "탈퇴", m.sn(), "회원 탈퇴", (LocalDateTime) m.get("WHDWL_DT"));
            }
            if (sns || s.chance(0.1)) {
                String offr = sns ? joinPath : c.of("USRG03", "카카오");
                s.store().add("SNS_ACNT_LINK", r -> r.set("MBR_SN", m.sn()).set("SNS_OFFR_CD", offr)
                        .set("OFFR_ACNT_ID", String.valueOf(3_000_000_000L + index * 7919L)).set("REG_DT", joinedAt));
            }
            for (Row term : s.store().rows("TRMS")) {
                boolean required = "Y".equals(term.get("ESNTL_AGRE_YN"));
                s.store().add("MBR_TRMS_AGRE", r -> r.set("MBR_SN", m.sn()).set("TRMS_SN", term.sn())
                        .set("AGRE_DT", joinedAt).yn("AGRE_YN", required || s.chance(0.4)).set("REG_DT", joinedAt));
            }
            if (index == 1 || s.chance(0.5)) {
                List<String[]> order = new ArrayList<>(List.of(WIDGETS));
                s.shuffle(order);
                for (int k = 0; k < order.size(); k++) {
                    String widget = c.of("MANG01", order.get(k)[0]);
                    int seq = (k + 1) * 10;
                    s.store().add("MBR_DSHBD_WDGT", r -> r.set("MBR_SN", m.sn()).set("DSHBD_WDGT_CD", widget)
                            .yn("INDCT_YN", s.chance(0.8)).set("SORT_SEQ", seq).set("REG_DT", joinedAt));
                }
            }
        }
    }

    private void history(Seed s, Codes c, long mbrSn, String before, String after, Long actor, String reason,
            LocalDateTime at) {
        s.store().add("MBR_STTS_HSTRY", r -> r.set("MBR_SN", mbrSn).set("PRCS_MBR_SN", actor)
                .set("BFR_STTS_CD", before == null ? null : c.of("USRG01", before)).set("AFTR_STTS_CD", c.of("USRG01", after))
                .set("CHG_RSN", reason).set("REG_DT", at));
    }

    private String nickname(Seed s, Set<String> used) {
        String base = s.pick(Fake.NICK_ADJ) + s.pick(Fake.NICK_NOUN);
        String nickname = base;
        for (int n = 2; used.contains(nickname); n++) {
            nickname = base + n;
        }
        used.add(nickname);
        return nickname;
    }
}
