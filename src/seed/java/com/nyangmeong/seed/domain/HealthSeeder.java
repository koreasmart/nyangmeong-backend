package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [건강기록]
 * - 일상기록 최근 30일(반려동물 70%), 관찰 항목, 일정, 검진기록(검사수치·기타검사·첨부·처방·반복예방), 케어 항목과 완료 이력
 * - CK_DAILY_HLTH_RCD_VAL_ONE : 배변상태만 코드, 나머지는 수치
 */
public class HealthSeeder implements Seeder {

    private record Insp(String code, String unit, double low, double high) {
    }

    private static final List<Insp> BLOOD = List.of(new Insp("WBC", "K/μL", 6.0, 17.0), new Insp("RBC", "M/μL", 5.5, 8.5),
            new Insp("HGB", "g/dL", 12.0, 18.0), new Insp("HCT", "%", 37.0, 55.0), new Insp("PLT", "K/μL", 175.0, 500.0),
            new Insp("BUN", "mg/dL", 7.0, 27.0), new Insp("CREA", "mg/dL", 0.5, 1.8), new Insp("ALT", "U/L", 10.0, 125.0),
            new Insp("AST", "U/L", 0.0, 50.0), new Insp("ALP", "U/L", 23.0, 212.0), new Insp("GLU", "mg/dL", 70.0, 143.0));

    private record Basic(String name, String zone, String cycle, int days, String icon) {
    }

    private static final List<Basic> BASICS = List.of(new Basic("양치하기", "일상관리", "1주", 7, "TOOTH"),
            new Basic("빗질하기", "일상관리", "1주", 7, "BRUSH"), new Basic("발톱 정리", "정기관리", "2주", 14, "NAIL"),
            new Basic("귀 청소", "정기관리", "2주", 14, "EAR"), new Basic("종합백신", "정기관리", "1년", 365, "VACCINE"));

    private static final List<String> OBSERVE_NAMES = List.of("작은 혹", "붉은 반점", "눈물 자국", "탈모 부위", "딱지", "부어오른 부위");
    private static final List<String> SCHEDULE_NAMES = List.of("미용 예약", "목욕", "심장사상충 약", "구충제 먹이기", "산책 모임", "사료 주문");
    private static final List<String> DRUGS = List.of("항생제", "소염진통제", "위장보호제", "피부 연고", "안약", "유산균");

    /** 회원 → 아직 검진기록에 연결하지 않은 건강기록 영수증 */
    private final Map<Long, Deque<Long>> receiptsByMember = new HashMap<>();

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        w.healthReceipts.forEach((recptSn, mbrSn) -> receiptsByMember.computeIfAbsent(mbrSn, k -> new ArrayDeque<>()).add(recptSn));
        List<World.Pet> live = w.livePets();
        for (World.Pet pet : live) {
            if (pet.ownerSn() == 1 || s.chance(0.7)) {
                daily(s, c, pet);
            }
            basicCare(s, c, pet);
            schedules(s, c, pet);
        }
        for (int i = 0; i < s.scaled(200); i++) {
            observe(s, c, s.pick(live));
        }
        for (int i = 0; i < s.scaled(1000); i++) {
            World.Pet pet = i < 6 ? first(w, live) : s.pick(live);
            checkup(s, c, pet);
        }
    }

    private World.Pet first(World w, List<World.Pet> live) {
        return live.stream().filter(p -> p.ownerSn() == 1).findFirst().orElseThrow();
    }

    private void daily(Seed s, Codes c, World.Pet pet) {
        double weight = pet.weight().doubleValue();
        double food = weight * (pet.dog() ? 20 : 15);
        for (int d = 29; d >= 0; d--) {
            LocalDate day = s.today().minusDays(d);
            if (d % 7 == 0) {
                weight += s.between(-0.05, 0.05) * weight / 10;
                record(s, c, pet, "체중", weight, null, day);
            }
            if (s.chance(0.8)) {
                record(s, c, pet, "식사량", food * s.between(0.8, 1.15), null, day);
            }
            if (s.chance(0.7)) {
                record(s, c, pet, "음수량", weight * s.between(35, 65), null, day);
            }
            if (s.chance(0.7)) {
                record(s, c, pet, "배변상태", null, s.weighted(List.of("정상", "딱딱함", "무름", "설사", "변비"), 80, 6, 7, 4, 3), day);
            }
            if (pet.dog() && s.chance(0.7)) {
                record(s, c, pet, "산책", (double) s.between(15, 90), null, day);
            }
        }
    }

    private void record(Seed s, Codes c, World.Pet pet, String indicator, Double value, String stool, LocalDate day) {
        LocalDateTime at = day.atTime(s.between(7, 22), s.between(0, 59));
        LocalDateTime when = at.isAfter(s.now()) ? s.now() : at;
        s.store().add("DAILY_HLTH_RCD", r -> r.set("PET_SN", pet.sn()).set("DAILY_RCD_IDCT_CD", c.of("HLTG09", indicator))
                .set("CRSD_STTS_CD", stool == null ? null : c.of("HLTG10", stool))
                .set("RCD_NVL", value == null ? null : s.decimal(value, 3)).set("RCD_DT", when).yn("DEL_YN", s.chance(0.02))
                .set("REG_DT", when));
    }

    private void observe(Seed s, Codes c, World.Pet pet) {
        boolean ended = s.chance(0.3);
        LocalDateTime start = s.daysAgo(10, 120);
        Row item = s.store().add("OBSRV_ARTCL", r -> r.set("PET_SN", pet.sn())
                .set("OBSRV_REGN_CD", s.pick(c.all("HLTG16"))).set("OBSRV_STTS_CD", c.of("HLTG18", ended ? "종료" : "관찰중"))
                .set("OBSRV_ARTCL_NM", s.pick(OBSERVE_NAMES)).set("OBSRV_REGN_DTL_NM", s.chance(0.4) ? "왼쪽 아래" : null)
                .set("OBSRV_END_YMD", ended ? s.today().minusDays(s.between(1, 9)) : null).yn("DEL_YN", s.chance(0.03))
                .set("REG_DT", start));
        s.store().add("OBSRV_ARTCL_STTS_HSTRY", r -> r.set("OBSRV_ARTCL_SN", item.sn()).set("MBR_SN", pet.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG18", "관찰중")).set("REG_DT", start));
        if (ended) {
            s.store().add("OBSRV_ARTCL_STTS_HSTRY", r -> r.set("OBSRV_ARTCL_SN", item.sn()).set("MBR_SN", pet.ownerSn())
                    .set("BFR_STTS_CD", c.of("HLTG18", "관찰중")).set("AFTR_STTS_CD", c.of("HLTG18", "종료"))
                    .set("REG_DT", ((LocalDate) item.get("OBSRV_END_YMD")).atTime(20, 0)));
        }
        double size = s.between(3.0, 15.0);
        LocalDateTime at = start;
        for (int k = 0, n = s.between(1, 4); k < n; k++) {
            long fileSn = s.images().add(SeedImages.Kind.OBSERVE);
            boolean firstCycle = k == 0;
            size = size * s.between(0.85, 1.15);
            double sz = size;
            LocalDateTime when = at;
            s.store().add("OBSRV_CYCL", r -> r.set("OBSRV_ARTCL_SN", item.sn()).set("ATCH_FILE_SN", fileSn)
                    .set("OBSRV_CHG_CD", firstCycle ? null : c.of("HLTG17", s.pick("작아짐", "그대로", "커짐")))
                    .set("OBSRV_SZ", s.decimal(sz, 3)).set("MEMO_CN", s.chance(0.5) ? "크기 변화 확인" : null)
                    .set("OBSRV_DT", when).set("REG_DT", when));
            at = s.soonAfter(at, 60 * 24 * 14);
        }
    }

    private void schedules(Seed s, Codes c, World.Pet pet) {
        for (int k = 0, n = s.between(2, 6); k < n; k++) {
            String kind = s.weighted(List.of("병원", "미용·목욕", "케어·투약", "기타"), 3, 3, 3, 2);
            LocalDate day = s.today().plusDays(s.between(-30, 45));
            boolean timed = s.chance(0.7);
            LocalDateTime at = timed ? day.atTime(s.between(9, 19), s.pick(0, 30)) : day.atStartOfDay();
            boolean hospital = "병원".equals(kind);
            LocalDateTime registered = at.minusDays(s.between(3, 20));
            LocalDateTime regDt = registered.isAfter(s.now()) ? s.now() : registered;
            s.store().add("SCHDL", r -> r.set("PET_SN", pet.sn()).set("SCHDL_KND_CD", c.of("HLTG19", kind))
                    .set("SCHDL_SRC_CD", c.of("HLTG22", s.chance(0.05) ? "AI 생성" : "직접 등록")).set("SCHDL_DT", at)
                    .set("SCHDL_NM", hospital ? s.pick("정기검진", "예방접종", "재진") : s.pick(SCHEDULE_NAMES))
                    .set("MEMO_CN", s.chance(0.3) ? "잊지 말기!" : null).yn("DEL_YN", s.chance(0.03)).yn("HR_INPT_YN", timed)
                    .set("HSPTL_NM", hospital ? s.pick(Fake.HOSPITALS) : null)
                    .set("CKUP_TYPE_CD", hospital ? c.of("HLTG01", s.pick("정기검진", "예방접종", "질병진료")) : null)
                    .set("REG_DT", regDt));
        }
    }

    private void checkup(Seed s, Codes c, World.Pet pet) {
        String type = s.weighted(List.of("정기검진", "예방접종", "질병진료", "검사", "기타 진료"), 25, 25, 30, 12, 8);
        LocalDate day = s.today().minusDays(s.between(1, 170));
        LocalDateTime at = day.atTime(s.between(9, 18), s.pick(0, 30));
        String hospital = s.pick(Fake.HOSPITALS);
        Row schedule = s.store().add("SCHDL", r -> r.set("PET_SN", pet.sn()).set("SCHDL_KND_CD", c.of("HLTG19", "병원"))
                .set("SCHDL_SRC_CD", c.of("HLTG22", "검진기록 연동")).set("SCHDL_DT", at).set("SCHDL_NM", type)
                .yn("HR_INPT_YN", true).set("HSPTL_NM", hospital).set("CKUP_TYPE_CD", c.of("HLTG01", type)).set("REG_DT", at));
        Deque<Long> receipts = receiptsByMember.get(pet.ownerSn());
        Long recptSn = receipts == null || receipts.isEmpty() || !s.chance(0.7) ? null : receipts.poll();
        boolean deleted = s.chance(0.05);
        LocalDateTime recordedAt = s.soonAfter(at, 60 * 6);
        Row record = s.store().add("CKUP_RCD", r -> r.set("RECPT_SN", recptSn).set("SCHDL_SN", schedule.sn())
                .set("CKUP_TYPE_CD", c.of("HLTG01", type)).set("CKUP_RCD_STTS_CD", c.of("HLTG02", deleted ? "삭제" : "완료"))
                .set("RCD_SRC_CD", c.of("HLTG03", recptSn == null ? "직접입력" : "영수증 OCR")).set("HSPTL_NM", hospital)
                .set("CKUP_CN", type + " 진행. " + s.pick("특이사항 없음.", "다음 내원 시 재검 권장.", "약 처방 후 경과 관찰."))
                .set("CKUP_MEMO_CN", s.chance(0.4) ? "보호자 메모: 컨디션 좋았음" : null).set("REG_DT", recordedAt));
        s.store().add("CKUP_RCD_STTS_HSTRY", r -> r.set("CKUP_RCD_SN", record.sn()).set("MBR_SN", pet.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG02", "완료")).set("REG_DT", recordedAt));
        if (deleted) {
            s.store().add("CKUP_RCD_STTS_HSTRY", r -> r.set("CKUP_RCD_SN", record.sn()).set("MBR_SN", pet.ownerSn())
                    .set("BFR_STTS_CD", c.of("HLTG02", "완료")).set("AFTR_STTS_CD", c.of("HLTG02", "삭제"))
                    .set("CHG_RSN", "잘못 등록함").set("REG_DT", s.after(recordedAt)));
        }
        if ("정기검진".equals(type) || "검사".equals(type)) {
            tests(s, c, record.sn(), recordedAt);
        }
        if (s.chance(0.4)) {
            attachment(s, c, record.sn(), type, recordedAt);
        }
        if ("질병진료".equals(type)) {
            for (int k = 0, n = s.between(1, 2); k < n; k++) {
                prescription(s, c, pet, record.sn(), day, k);
            }
        }
        if ("예방접종".equals(type) && s.chance(0.5)) {
            prevention(s, c, pet, record.sn(), day);
        }
    }

    private void tests(Seed s, Codes c, long recordSn, LocalDateTime at) {
        int seq = 0;
        for (Insp insp : s.sample(BLOOD, s.between(5, BLOOD.size()))) {
            double range = insp.high() - insp.low();
            double value = s.chance(0.85) ? insp.low() + range * s.between(0.1, 0.9)
                    : s.chance(0.5) ? insp.high() + range * s.between(0.05, 0.3) : Math.max(0, insp.low() - range * s.between(0.05, 0.2));
            int order = seq += 10;
            s.store().add("INSP_NVL", r -> r.set("CKUP_RCD_SN", recordSn).set("INSP_ARTCL_CD", c.of("HLTG12", insp.code()))
                    .set("UNIT_NM", insp.unit()).set("RSLT_NVL", s.decimal(value, 4)).set("REF_LLMT_NVL", s.decimal(insp.low(), 4))
                    .set("REF_UPLMT_NVL", s.decimal(insp.high(), 4)).set("SORT_SEQ", order).set("REG_DT", at));
        }
        if (s.chance(0.3)) {
            int order = seq + 10;
            s.store().add("INSP_NVL", r -> r.set("CKUP_RCD_SN", recordSn).set("INSP_ARTCL_CD", c.of("HLTG12", "직접입력"))
                    .set("INSP_ARTCL_DRCT_INPT_NM", "SDMA").set("UNIT_NM", "μg/dL").set("RSLT_NVL", s.decimal(s.between(5.0, 18.0), 4))
                    .set("REF_LLMT_NVL", s.decimal(0, 4)).set("REF_UPLMT_NVL", s.decimal(14, 4)).set("SORT_SEQ", order)
                    .set("REG_DT", at));
        }
        if (s.chance(0.2)) {
            s.store().add("ETC_INSP", r -> r.set("CKUP_RCD_SN", recordSn)
                    .set("INSP_NM", s.pick("심장사상충 키트 검사", "소변검사", "피부 스크래핑")).set("INSP_RSLT_CN", s.pick("음성", "정상 범위", "경미한 이상"))
                    .set("REG_DT", at));
        }
    }

    private void attachment(Seed s, Codes c, long recordSn, String type, LocalDateTime at) {
        String attachType = "예방접종".equals(type) ? "접종 증명서" : s.chance(0.7) ? "검사결과지" : "영상 사진";
        boolean scan = "영상 사진".equals(attachType);
        long fileSn = s.images().add(scan ? SeedImages.Kind.SCAN : SeedImages.Kind.DOCUMENT);
        s.store().add("CKUP_RCD_ATCH", r -> r.set("CKUP_RCD_SN", recordSn).set("ATCH_FILE_SN", fileSn)
                .set("ATCH_TYPE_CD", c.of("HLTG20", attachType)).set("IMG_KND_CD", scan ? s.pick(c.all("HLTG23")) : null)
                .set("SHT_REGN_CD", scan ? s.pick(c.all("HLTG24")) : null).set("REG_DT", at));
    }

    private void prescription(Seed s, Codes c, World.Pet pet, long recordSn, LocalDate day, int k) {
        LocalDate end = day.plusDays(s.between(3, 14));
        boolean taking = !end.isBefore(s.today());
        boolean reflect = s.chance(0.5);
        String drug = DRUGS.get((int) ((recordSn + k) % DRUGS.size()));
        Row prsc = s.store().add("PRSC", r -> r.set("CKUP_RCD_SN", recordSn).set("PRSC_STTS_CD", c.of("HLTG06", taking ? "복용중" : "종료"))
                .set("MDCN_NM", drug).set("DAY_MDCN_NMTM", s.between(1, 3)).set("MDCN_BGNG_YMD", day).set("MDCN_END_YMD", end)
                .yn("CARE_RFLT_YN", reflect).set("REG_DT", day.atTime(19, 0)));
        s.store().add("PRSC_STTS_HSTRY", r -> r.set("PRSC_SN", prsc.sn()).set("MBR_SN", pet.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG06", "복용중")).set("REG_DT", day.atTime(19, 0)));
        if (!taking) {
            s.store().add("PRSC_STTS_HSTRY", r -> r.set("PRSC_SN", prsc.sn()).set("BFR_STTS_CD", c.of("HLTG06", "복용중"))
                    .set("AFTR_STTS_CD", c.of("HLTG06", "종료")).set("CHG_RSN", "복용 기간 종료").set("REG_DT", end.atTime(23, 59)));
        }
        if (reflect) {
            Row care = s.store().add("CARE_ARTCL", r -> r.set("PRSC_SN", prsc.sn()).set("PET_SN", pet.sn())
                    .set("CARE_ZONE_CD", c.of("HLTG13", "일상관리")).set("CARE_ARTCL_NM", drug + " 복용").set("RPRS_ICON_ID", "PILL")
                    .set("LAST_CMPTN_YMD", taking ? s.today().minusDays(1) : end).yn("DEL_YN", !taking && s.chance(0.5))
                    .set("REG_DT", day.atTime(19, 0)));
            LocalDate stop = taking ? s.today().minusDays(1) : end;
            int cycle = 0;
            for (LocalDate d = day; !d.isAfter(stop); d = d.plusDays(1)) {
                completion(s, care.sn(), pet.ownerSn(), ++cycle, d);
            }
        }
    }

    private void prevention(Seed s, Codes c, World.Pet pet, long recordSn, LocalDate day) {
        String item = s.pick("심장사상충 예방", "구충제");
        Row mng = s.store().add("REPT_PRVNT_MNG", r -> r.set("PET_SN", pet.sn()).set("CKUP_RCD_SN", recordSn)
                .set("REPT_PRVNT_ARTCL_CD", c.of("HLTG07", item)).set("REPT_CYCLE_CD", c.of("HLTG15", "한달")).set("BGNG_YMD", day)
                .set("REG_DT", day.atTime(19, 0)));
        long months = ChronoUnit.MONTHS.between(day, s.today());
        LocalDate last = day.plusMonths(months);
        Row care = s.store().add("CARE_ARTCL", r -> r.set("REPT_PRVNT_MNG_SN", mng.sn()).set("PET_SN", pet.sn())
                .set("CARE_ZONE_CD", c.of("HLTG13", "정기관리")).set("CARE_CYCLE_CD", c.of("HLTG15", "한달")).set("CARE_ARTCL_NM", item)
                .set("RPRS_ICON_ID", "HEART").set("LAST_CMPTN_YMD", last).set("NXTM_PRNMNT_YMD", last.plusMonths(1))
                .set("REG_DT", day.atTime(19, 0)));
        for (int m = 0; m <= months; m++) {
            completion(s, care.sn(), pet.ownerSn(), m + 1, day.plusMonths(m));
        }
    }

    private void basicCare(Seed s, Codes c, World.Pet pet) {
        List<Basic> items = new ArrayList<>(s.sample(BASICS, s.between(2, 4)));
        int mainSeq = 0;
        for (Basic b : items) {
            boolean main = mainSeq < 30 && s.chance(0.6);
            int seq = main ? (mainSeq += 10) : 0;
            LocalDate last = s.today().minusDays(s.between(0, Math.min(b.days(), 30)));
            Row care = s.store().add("CARE_ARTCL", r -> r.set("PET_SN", pet.sn()).set("CARE_ZONE_CD", c.of("HLTG13", b.zone()))
                    .set("CARE_CYCLE_CD", c.of("HLTG15", b.cycle())).set("CARE_ARTCL_NM", b.name()).set("RPRS_ICON_ID", b.icon())
                    .set("LAST_CMPTN_YMD", last).set("NXTM_PRNMNT_YMD", last.plusDays(b.days())).yn("MAIN_EXPSR_YN", main)
                    .set("MAIN_SORT_SEQ", seq).yn("BSC_ARTCL_YN", true).set("REG_DT", pet.registeredAt()));
            int cycle = 0;
            for (LocalDate d = last.minusDays(b.days() * 4L); !d.isAfter(last); d = d.plusDays(b.days())) {
                if (!d.isBefore(pet.registeredAt().toLocalDate())) {
                    completion(s, care.sn(), pet.ownerSn(), ++cycle, d);
                }
            }
        }
    }

    private void completion(Seed s, long careSn, long mbrSn, int cycle, LocalDate day) {
        LocalDateTime at = day.atTime(21, 0).isAfter(s.now()) ? s.now() : day.atTime(21, 0);
        s.store().add("CARE_CMPTN_HSTRY", r -> r.set("CARE_ARTCL_SN", careSn).set("MBR_SN", mbrSn).set("CMPTN_CYCL", cycle)
                .set("CMPTN_YMD", day).set("REG_DT", at));
    }
}
