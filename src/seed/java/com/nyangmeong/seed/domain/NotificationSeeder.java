package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [인앱 알림]
 * - 정상 회원마다 최근 10일 동안 약 20개(1번 30개). AUTO_HIDE_DT = 등록 + SYS_STNG.NOTI_HIDE_CRTR_DAY_CNT(7일)
 * - 7일보다 오래된 알림은 숨김 상태라 목록에 나오지 않는다(숨김 확인용)
 */
public class NotificationSeeder implements Seeder {

    private static final int HIDE_DAYS = 7;

    /** petOnly : 반려동물이 없는 회원에게는 보내지 않는 유형 */
    private record Template(String type, String title, String path, boolean petOnly) {

        Template(String type, String title, String path) {
            this(type, title, path, List.of("케어 일정", "공동양육", "캐릭터").contains(type));
        }
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("케어 일정", "%s의 병원 일정이 내일이에요", "/health/calendar"),
            new Template("펫돌봄 거래 상태", "펫돌봄 매칭 요청이 도착했어요", "/chat"),
            new Template("커뮤니티", "내 글에 새 댓글이 달렸어요", "/community"),
            new Template("공동양육", "공동양육 초대가 수락되었어요", "/mypage/pets"),
            new Template("실종 만료", "실종 제보 게시 기간이 3일 남았어요", "/community/missing"),
            new Template("가계부", "이번 달 지출이 한도의 80%%를 넘었어요", "/mypage/expenses"),
            new Template("캐릭터", "%s의 캐릭터가 완성됐어요", "/mypage/characters"),
            new Template("공지", "새 공지사항이 올라왔어요", "/notices"),
            new Template("1:1 문의 답변", "문의하신 내용에 답변이 등록됐어요", "/inquiries"));

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        List<World.Post> posts = w.visiblePosts();
        for (World.Member m : w.members) {
            Row mbr = s.store().row("MBR", m.sn());
            if (m.status() != World.MemberStatus.ACTIVE || "N".equals(mbr.get("INAPP_NOTI_RCPTN_YN"))) {
                continue;
            }
            Long petSn = w.representativePet.get(m.sn());
            String petName = petSn == null ? "" : w.pet(petSn).name();
            List<Template> templates = TEMPLATES.stream().filter(t -> petSn != null || !t.petOnly()).toList();
            List<World.Post> mine = posts.stream().filter(p -> p.mbrSn() == m.sn()).toList();
            int count = m.sn() == 1 ? 30 : s.between(10, 30);
            for (int i = 0; i < count; i++) {
                Template t = s.pick(templates);
                LocalDateTime at = s.daysAgo(0, 10);
                World.Post post = mine.isEmpty() ? s.pick(posts) : s.pick(mine);
                String title = t.title().contains("%s") ? String.format(t.title(), petName) : String.format(t.title());
                String path = t.path().contains("%d") ? String.format(t.path(), post.sn()) : t.path();
                boolean read = at.isBefore(s.now().minusDays(2)) ? s.chance(0.8) : s.chance(0.3);
                s.store().add("INAPP_NOTI", r -> r.set("MBR_SN", m.sn()).set("NOTI_TYPE_CD", c.of("AIMG02", t.type()))
                        .set("NOTI_TTL", title).set("NOTI_CN", s.chance(0.5) ? "자세한 내용은 눌러서 확인해 주세요." : null).set("MVMN_PATH", path)
                        .set("PRSL_DT", read ? s.soonAfter(at, 60 * 12) : null).set("AUTO_HIDE_DT", at.plusDays(HIDE_DAYS))
                        .set("REG_DT", at));
            }
        }
    }
}
