package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Fake;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedImages;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [고객센터·운영]
 * - NTC_MTTR 30, FAQ 40, NEWS 50, OTO_INQRY 배율 × 200(픽셀 캐릭터 문의는 캐릭터·사진 연결, 무료 재생성이면 새 생성 작업)
 */
public class SupportSeeder implements Seeder {

    private static final List<String> NOTICES = List.of("서비스 정기 점검 안내", "개인정보 처리방침 개정 안내", "픽셀 캐릭터 기능 오픈",
            "커뮤니티 운영정책 안내", "추석 연휴 고객센터 운영 안내", "케어미션 포인트 정책 변경", "앱 업데이트 안내", "펫돌봄 이용 수칙 안내");
    private static final List<List<String>> FAQS = List.of(
            List.of("이용방법", "반려동물은 몇 마리까지 등록할 수 있나요?", "계정당 최대 10마리까지 등록할 수 있어요."),
            List.of("이용방법", "케어미션 포인트는 언제 들어오나요?", "미션을 완료한 다음 날 정산되어 들어와요."),
            List.of("이용방법", "공동양육자는 어떻게 초대하나요?", "마이페이지 > 반려동물 관리에서 이메일로 초대할 수 있어요."),
            List.of("계정", "비밀번호를 잊어버렸어요.", "로그인 화면의 비밀번호 찾기에서 이메일 인증 후 재설정할 수 있어요."),
            List.of("계정", "탈퇴하면 데이터는 어떻게 되나요?", "30일 동안 보관된 뒤 삭제돼요. 그 안에 복구할 수 있어요."),
            List.of("신고/제재", "신고가 누적되면 어떻게 되나요?", "게시글은 신고 5회 이상이면 블라인드 처리돼요."),
            List.of("기타", "펫돌봄 비용은 어떻게 정하나요?", "모집 글에 시급 또는 건당 금액을 적고 채팅으로 조율해요."),
            List.of("기타", "실종 제보는 언제 만료되나요?", "최종 목격일로부터 30일이 지나면 만료돼요. 연장할 수 있어요."));
    private static final List<String> PRESSES = List.of("펫뉴스", "반려동물신문", "데일리펫", "애니멀타임즈");

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        for (int i = 0; i < 30; i++) {
            int index = i;
            s.store().add("NTC_MTTR", r -> r.set("NTC_MTTR_TTL", NOTICES.get(index % NOTICES.size()) + (index >= NOTICES.size() ? " (" + (index / NOTICES.size() + 1) + ")" : ""))
                    .set("NTC_MTTR_CN", Fake.paragraph(s, 2, 4)).set("INQ_CNT", s.between(10, 3000)).yn("UPEND_FIX_YN", index < 3)
                    .yn("USE_YN", index < 28).set("REG_DT", s.daysAgo(index * 5, index * 5 + 4)));
        }
        for (int i = 0; i < 40; i++) {
            List<String> faq = FAQS.get(i % FAQS.size());
            int seq = (i + 1) * 10;
            String suffix = i >= FAQS.size() ? " (" + (i / FAQS.size() + 1) + ")" : "";
            s.store().add("FAQ", r -> r.set("FAQ_CLSF_CD", c.of("CSCG01", faq.get(0))).set("QSTN_TTL", faq.get(1) + suffix)
                    .set("ANS_CN", faq.get(2)).set("SORT_SEQ", seq).set("REG_DT", s.daysAgo(100, 200)));
        }
        news(s, c);
        inquiries(s, w, c);
    }

    private void news(Seed s, Codes c) {
        for (int i = 1; i <= 50; i++) {
            boolean external = s.chance(0.7);
            Long thumb = external ? null : s.images().add(SeedImages.Kind.NEWS);
            int index = i;
            s.store().add("NEWS", r -> r.set("ATCH_FILE_SN", thumb).set("NEWS_CLSF_CD", s.pick(c.all("CMUG06")))
                    .set("NEWS_SRC_CD", c.of("CMUG07", external ? "외부 언론사" : "관리자 등록")).set("NEWS_TTL", s.pick(Fake.INFO_TITLES) + " 관련 소식")
                    .set("NEWS_CN", Fake.paragraph(s, 3, 6)).set("SRC_PRESS_NM", external ? s.pick(PRESSES) : null)
                    .set("ORGTXT_PATH", external ? "https://news.example.com/pet/" + (20260000 + index) : null)
                    .set("THMB_URL_PATH", external ? "https://news.example.com/pet/" + (20260000 + index) + "/thumb.jpg" : null)
                    .set("PBLCN_DT", s.daysAgo(0, 60)).set("REG_DT", s.daysAgo(0, 60)));
        }
    }

    private void inquiries(Seed s, World w, Codes c) {
        List<Row> chars = s.store().rows("PIXEL_CHAR").stream()
                .filter(ch -> w.characterPhoto.containsKey(ch.sn()) && w.member((Long) ch.get("MBR_SN")).active()).toList();
        List<World.Member> people = w.activeMembers();
        List<World.Member> admins = w.admins();
        String charType = c.of("CSCG02", "픽셀 캐릭터");
        List<String> otherTypes = c.all("CSCG02").stream().filter(t -> !t.equals(charType)).toList();
        for (int i = 0; i < s.scaled(200); i++) {
            boolean character = s.chance(0.2) && !chars.isEmpty();
            Row ch = character ? s.pick(chars) : null;
            World.Member m = ch != null ? w.member((Long) ch.get("MBR_SN")) : i < 5 ? w.member(1) : s.pick(people);
            boolean answered = s.chance(0.6);
            World.Member admin = s.pick(admins);
            LocalDateTime at = s.after(m.joinedAt());
            LocalDateTime answeredAt = answered ? s.after(at) : null;
            String result = ch != null && answered ? s.pick("무료 재생성", "원래 캐릭터 복원") : null;
            Row inquiry = s.store().add("OTO_INQRY", r -> r.set("MBR_SN", m.sn()).set("PRCS_MBR_SN", answered ? admin.sn() : null)
                    .set("PIXEL_CHAR_SN", ch == null ? null : ch.sn()).set("CHAR_PHOTO_SN", ch == null ? null : w.characterPhoto.get(ch.sn()))
                    .set("INQRY_TYPE_CD", ch != null ? charType : s.pick(otherTypes))
                    .set("INQRY_PRCS_STTS_CD", c.of("CSCG03", answered ? "답변 완료" : "접수"))
                    .set("CHAR_ERR_PRCS_RSLT_CD", result == null ? null : c.of("CHRG04", result))
                    .set("INQRY_TTL", ch != null ? "캐릭터가 이상하게 나왔어요" : s.pick("포인트가 안 들어왔어요", "비밀번호 변경이 안 돼요", "장소 정보가 틀려요",
                            "게시글 신고 문의", "가계부 금액 수정 문의"))
                    .set("INQRY_CN", "안녕하세요. " + Fake.paragraph(s, 1, 2) + " 확인 부탁드립니다.")
                    .set("ANS_CN", answered ? "안녕하세요, 냥멍케어입니다. 문의하신 내용을 확인해 처리했어요. 이용에 불편을 드려 죄송합니다." : null)
                    .set("ANS_DT", answeredAt).set("REG_DT", at));
            s.store().add("OTO_INQRY_STTS_HSTRY", r -> r.set("OTO_INQRY_SN", inquiry.sn()).set("MBR_SN", m.sn())
                    .set("AFTR_STTS_CD", c.of("CSCG03", "접수")).set("REG_DT", at));
            if (answered) {
                s.store().add("OTO_INQRY_STTS_HSTRY", r -> r.set("OTO_INQRY_SN", inquiry.sn()).set("MBR_SN", admin.sn())
                        .set("BFR_STTS_CD", c.of("CSCG03", "접수")).set("AFTR_STTS_CD", c.of("CSCG03", "답변 완료")).set("REG_DT", answeredAt));
            }
            if (s.chance(0.3)) {
                for (int k = 0, n = s.between(1, 2); k < n; k++) {
                    long fileSn = s.images().add(SeedImages.Kind.INQUIRY);
                    int seq = (k + 1) * 10;
                    s.store().add("INQRY_ATCH_PHOTO", r -> r.set("OTO_INQRY_SN", inquiry.sn()).set("ATCH_FILE_SN", fileSn).set("SORT_SEQ", seq)
                            .set("REG_DT", at));
                }
            }
            if ("무료 재생성".equals(result)) {
                regenerate(s, c, ch, inquiry.sn(), answeredAt);
            }
        }
    }

    /**
     * 무료 재생성 요청 작업(문의 연결). 결과가 나오기 전이라 사진은 없다
     */
    private void regenerate(Seed s, Codes c, Row ch, long inquirySn, LocalDateTime at) {
        Row job = s.store().add("CHAR_CRT_JOB", r -> r.set("PET_SN", ch.get("PET_SN")).set("MBR_SN", ch.get("MBR_SN"))
                .set("OTO_INQRY_SN", inquirySn).set("CRT_JOB_STTS_CD", c.of("CHRG01", "요청")).set("IDMPT_DMND_ID", s.uuid())
                .set("REG_DT", at));
        s.store().add("CHAR_CRT_JOB_STTS_HSTRY", r -> r.set("CHAR_CRT_JOB_SN", job.sn()).set("MBR_SN", ch.get("MBR_SN"))
                .set("AFTR_STTS_CD", c.of("CHRG01", "요청")).set("CHG_RSN", "1:1 문의 무료 재생성").set("REG_DT", at));
    }
}
