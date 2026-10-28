package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [AI 대화]
 * - AI_CNVRS_ROOM 배율 × 100, 방마다 사용자·AI 메시지가 번갈아 2~8개. 제목·내용은 암호문
 */
public class AiSeeder implements Seeder {

    private static final List<String[]> TALKS = List.of(
            new String[] { "요즘 밥을 남겨요", "최근 식사량이 줄었다면 간식 양과 활동량을 함께 확인해 보세요. 3일 이상 계속되면 병원 진료를 권해요." },
            new String[] { "산책은 얼마나 해야 하나요?", "체중과 나이에 따라 다르지만 하루 2번, 30분 정도가 일반적이에요." },
            new String[] { "물을 너무 많이 마셔요", "음수량이 갑자기 늘었다면 신장이나 내분비 질환일 수 있어요. 기록을 보며 수의사와 상담해 주세요." },
            new String[] { "양치는 매일 해야 하나요?", "가능하면 매일, 어렵다면 주 3회 이상을 권해요." });

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        FieldCryptoService crypto = s.crypto();
        List<World.Member> owners = w.activeMembers().stream().filter(m -> w.representativePet.containsKey(m.sn())).toList();
        for (int i = 0; i < s.scaled(100); i++) {
            World.Member m = i < 3 ? w.member(1) : s.pick(owners);
            String[] topic = s.pick(TALKS);
            LocalDateTime at = s.daysAgo(0, 30);
            Long petSn = s.chance(0.8) ? w.representativePet.get(m.sn()) : null;
            Row room = s.store().add("AI_CNVRS_ROOM", r -> r.set("MBR_SN", m.sn()).set("PET_SN", petSn)
                    .set("CNVRS_TTL_ENC", crypto.encrypt(topic[0])).set("REG_DT", at));
            int turns = s.between(1, 4);
            LocalDateTime t = at;
            for (int k = 0; k < turns * 2; k++) {
                boolean user = k % 2 == 0;
                int seq = k + 1;
                boolean incomplete = !user && k == turns * 2 - 1 && s.chance(0.05);
                String text = k < 2 ? topic[user ? 0 : 1] : user ? "조금 더 자세히 알려 주세요" : "기록된 체중과 식사량을 보면 큰 변화는 없어요. 며칠 더 지켜봐 주세요.";
                LocalDateTime when = t;
                s.store().add("AI_CNVRS_MSG", r -> r.set("AI_CNVRS_ROOM_SN", room.sn()).set("ROLE_CD", c.of("AIMG06", user ? "사용자" : "AI"))
                        .set("CNVRS_CN_ENC", crypto.encrypt(text)).set("MSG_SEQ", seq).yn("CMPTN_YN", !incomplete).set("REG_DT", when));
                t = s.soonAfter(t, 2);
            }
        }
    }
}
