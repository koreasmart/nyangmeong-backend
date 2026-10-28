package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * [영수증·가계부]
 * - RECPT 배율 × 500(상태별 이력 포함), EXPND 배율 × 10,000(최근 6개월)
 * - OCR_RSPNS_JSON은 global/external/ReceiptOcrResponse를 Jackson으로 쓴 모양과 같다
 */
public class ExpenseSeeder implements Seeder {

    private record Category(String name, int min, int max, List<String> details) {
    }

    private static final List<Category> CATEGORIES = List.of(
            new Category("병원", 20_000, 300_000, List.of("정기검진", "예방접종", "피부 진료", "치석 제거", "혈액검사")),
            new Category("사료", 20_000, 90_000, List.of("건식 사료 6kg", "습식 사료 12개입", "처방식 사료", "그레인프리 사료")),
            new Category("간식", 5_000, 30_000, List.of("져키", "동결건조 간식", "츄르 20개입", "덴탈껌")),
            new Category("용품", 5_000, 80_000, List.of("배변패드", "장난감", "하네스", "모래", "스크래쳐", "방석")),
            new Category("미용", 30_000, 120_000, List.of("전체 미용", "목욕", "발톱 정리", "위생 미용")),
            new Category("돌봄", 20_000, 100_000, List.of("펫시터", "애견유치원", "호텔 1박")),
            new Category("기타", 5_000, 50_000, List.of("펫보험", "등록 수수료", "교육")));

    /** 최종 상태 → 거쳐 온 상태(처음은 분석중) */
    private static final Map<String, List<String>> PATHS = Map.of(
            "분석중", List.of("분석중"),
            "인식성공", List.of("분석중", "인식성공"),
            "부분인식", List.of("분석중", "부분인식"),
            "인식실패", List.of("분석중", "인식실패"),
            "일시오류", List.of("분석중", "일시오류"),
            "취소", List.of("분석중", "인식성공", "취소"),
            "저장완료", List.of("분석중", "인식성공", "저장완료"));

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        List<World.Member> owners = w.activeMembers().stream().filter(m -> !w.caredPets.getOrDefault(m.sn(), List.of()).isEmpty())
                .toList();
        receipts(s, w, c, owners);
        manualExpenses(s, w, c, owners);
    }

    private void receipts(Seed s, World w, Codes c, List<World.Member> owners) {
        List<String> finals = List.of("저장완료", "인식성공", "부분인식", "인식실패", "취소", "일시오류", "분석중");
        for (int i = 0; i < s.scaled(500); i++) {
            World.Member m = i < 10 ? w.member(1) : s.pick(owners);
            List<Long> pets = w.caredPets.get(m.sn());
            boolean health = s.chance(0.3);
            String status = s.weighted(finals, 70, 7, 5, 8, 5, 3, 2);
            LocalDateTime at = "분석중".equals(status) ? s.now().minusMinutes(s.between(1, 30)) : s.daysAgo(1, 170);
            long fileSn = s.images().add(SeedImages.Kind.RECEIPT);
            String hospital = s.pick(Fake.HOSPITALS);
            long petSn = s.pick(pets);
            String petName = w.pet(petSn).name();
            int amount = s.between(3, 40) * 5_000;
            boolean recognized = List.of("인식성공", "부분인식", "저장완료", "취소").contains(status);
            Row receipt = s.store().add("RECPT", r -> {
                r.set("MBR_SN", m.sn()).set("ATCH_FILE_SN", fileSn).set("OCR_PRCS_STTS_CD", c.of("AIMG01", status))
                        .set("OCR_DMND_SRC_CD", c.of("AIMG07", health ? "건강기록" : "가계부")).set("IDMPT_DMND_ID", s.uuid())
                        .set("REG_DT", at);
                if (recognized) {
                    r.set("OCR_RSPNS_JSON", ocrJson(s, hospital, at.toLocalDate(), amount, petName));
                }
                if ("저장완료".equals(status)) {
                    r.set("STRG_DT", s.soonAfter(at, 30));
                }
            });
            LocalDateTime t = at;
            String before = null;
            for (String step : PATHS.get(status)) {
                String bfr = before;
                LocalDateTime stepAt = t;
                s.store().add("RECPT_STTS_HSTRY", r -> r.set("RECPT_SN", receipt.sn()).set("MBR_SN", m.sn())
                        .set("BFR_STTS_CD", bfr == null ? null : c.of("AIMG01", bfr)).set("AFTR_STTS_CD", c.of("AIMG01", step))
                        .set("CHG_RSN", "인식실패".equals(step) ? "영수증 글자를 읽지 못했어요" : null).set("REG_DT", stepAt));
                before = step;
                t = s.soonAfter(t, 5);
            }
            if (!"저장완료".equals(status)) {
                continue;
            }
            if (health) {
                w.healthReceipts.put(receipt.sn(), m.sn());
            }
            LocalDate ymd = at.toLocalDate();
            s.store().add("EXPND", r -> r.set("PET_SN", petSn).set("MBR_SN", m.sn()).set("RECPT_SN", receipt.sn())
                    .set("EXPND_CLSF_CD", c.of("MYPG05", "병원")).set("EXPND_INPT_MTH_CD", c.of("MYPG06", "OCR"))
                    .set("EXPND_AMT", amount).set("DTL_MTTR", hospital + " 진료").set("EXPND_YMD", ymd)
                    .set("REG_DT", receipt.get("STRG_DT")));
        }
    }

    private void manualExpenses(Seed s, World w, Codes c, List<World.Member> owners) {
        int petTotal = owners.stream().mapToInt(m -> w.caredPets.get(m.sn()).size()).sum();
        int target = s.scaled(10_000);
        String manual = c.of("MYPG06", "직접입력");
        for (World.Member m : owners) {
            List<Long> pets = w.caredPets.get(m.sn());
            int n = Math.max(1, Math.round((float) target * pets.size() / petTotal)) + (m.sn() == 1 ? 40 : 0);
            for (int i = 0; i < n; i++) {
                Category cat = s.weighted(CATEGORIES, 15, 20, 25, 20, 8, 4, 8);
                // 1번 회원은 이번 달 지출이 보이게 절반을 최근 30일에 둔다
                LocalDate ymd = m.sn() == 1 && i % 2 == 0 ? s.today().minusDays(s.between(0, 29))
                        : s.today().minusDays(s.between(0, 179));
                LocalDateTime at = ymd.atTime(s.between(9, 23), s.between(0, 59));
                Long petSn = s.chance(0.1) ? null : s.pick(pets);
                s.store().add("EXPND", r -> r.set("PET_SN", petSn).set("MBR_SN", m.sn())
                        .set("EXPND_CLSF_CD", c.of("MYPG05", cat.name())).set("EXPND_INPT_MTH_CD", manual)
                        .set("EXPND_AMT", s.between(cat.min() / 1000, cat.max() / 1000) * 1000)
                        .set("DTL_MTTR", s.chance(0.8) ? s.pick(cat.details()) : null).set("EXPND_YMD", ymd)
                        .yn("DEL_YN", s.chance(0.03)).set("REG_DT", at.isAfter(s.now()) ? s.now() : at));
            }
        }
    }

    /**
     * ReceiptOcrResponse JSON(반려동물 구역 1개)
     */
    private String ocrJson(Seed s, String hospital, LocalDate ymd, int amount, String petName) {
        int drug = s.chance(0.5) ? s.between(1, 4) * 5_000 : 0;
        int items = amount - drug;
        int exam = items / 2;
        String drugs = drug == 0 ? "[]"
                : "[{\"mdcnNm\":\"" + s.pick("항생제", "소염제", "위장약", "피부약") + "\",\"mdcnDays\":" + s.between(3, 14)
                        + ",\"mdcnUsage\":\"1일 2회\",\"mdcnAmt\":" + drug + "}]";
        return "{\"hsptlNm\":\"" + hospital + "\",\"ckupYmd\":\"" + ymd + "\",\"recptAmt\":" + amount
                + ",\"btmDscntAmt\":0,\"sections\":[{\"sectionNm\":\"" + petName + "\",\"sbttlAmt\":" + amount
                + ",\"dscntAmt\":0,\"items\":[{\"itemNm\":\"진찰료\",\"itemAmt\":" + exam + "},{\"itemNm\":\""
                + s.pick("혈액검사", "피부검사", "주사", "처치료") + "\",\"itemAmt\":" + (items - exam) + "}],\"drugs\":" + drugs
                + "}]}";
    }
}
