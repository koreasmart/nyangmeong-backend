package com.nyangmeong.seed;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nyangmeong.seed.core.Schema;

/**
 * [시드 대상 테이블]
 * - SEEDED : 도메인 Seeder가 채우는 테이블
 * - RUNTIME : 실행 중 쌓이는 기록. 넣지 않고, 비울 때만 함께 비운다(지운 회원을 가리키지 않게)
 * - KEPT : 넣지도 비우지도 않는다. COM_CD(기준 SQL), NOTI_TMPLT(없으면 코드의 기본 문구)
 * - 그 밖에 DB에 있는 테이블(새로 생긴 테이블)은 자동 엔진(AutoFill)이 채우고 비울 때도 비운다
 */
public final class SeedTables {

    public static final List<String> SEEDED = List.of(
            "SYS_STNG", "TRMS", "RGN", "VRTY", "ATCH_FILE", "CARE_MSN", "ITEM", "CMNTY_AI_CMTRY",
            "MBR", "MBR_STTS_HSTRY", "SNS_ACNT_LINK", "MBR_TRMS_AGRE", "MBR_DSHBD_WDGT",
            "PET", "BRUP_REL", "BRUP_LINK_STTS_HSTRY", "JNT_BRUP_INVTN", "INVTN_TRGT_PET", "JNT_BRUP_INVTN_STTS_HSTRY",
            "PLC", "PLC_BKMK",
            "RECPT", "RECPT_STTS_HSTRY", "EXPND",
            "DAILY_HLTH_RCD", "OBSRV_ARTCL", "OBSRV_ARTCL_STTS_HSTRY", "OBSRV_CYCL", "SCHDL",
            "CKUP_RCD", "CKUP_RCD_STTS_HSTRY", "CKUP_RCD_ATCH", "INSP_NVL", "ETC_INSP", "PRSC", "PRSC_STTS_HSTRY",
            "REPT_PRVNT_MNG", "CARE_ARTCL", "CARE_CMPTN_HSTRY",
            "PIXEL_CHAR", "CHAR_CRT_JOB", "CHAR_CRT_JOB_STTS_HSTRY", "CHAR_PHOTO", "HLD_ITEM", "DAY_MSN", "PNT_GNLGR",
            "PST", "PST_STTS_HSTRY", "PST_IMG", "PST_RCMDTN", "CMNT", "DCLR", "DCLR_STTS_HSTRY",
            "LOST_TPOF", "LOST_TPOF_STTS_HSTRY",
            "PET_PRTC_RCRT", "PET_PRTC_RCRT_STTS_HSTRY", "RCRT_PET_KND", "RCRT_PRTC_SHP",
            "OTO_CHAT_ROOM", "OTO_CHAT_ROOM_STTS_HSTRY", "CHAT_PRTPNT", "CHAT_PRTPNT_STTS_HSTRY", "CHAT_MSG",
            "CHAT_MSG_PHOTO", "PET_PRTC_MTCH", "PET_PRTC_MTCH_STTS_HSTRY", "MTCH_CND_VER", "PRTC_CMPTN_DMND_CYCL",
            "PRTC_CMPTN_PHOTO",
            "NEWS", "NTC_MTTR", "FAQ", "OTO_INQRY", "OTO_INQRY_STTS_HSTRY", "INQRY_ATCH_PHOTO",
            "INAPP_NOTI", "AI_CNVRS_ROOM", "AI_CNVRS_MSG");

    public static final List<String> RUNTIME = List.of("RFRSH_TKN", "LGN_ATMPT_LMT", "EML_CERT", "NOTI_SNDNG_LOG");

    public static final List<String> KEPT = List.of("COM_CD", "NOTI_TMPLT");

    private SeedTables() {
    }

    /**
     * 넣는 테이블: SEEDED 중 DB에 있는 것 + DB에만 있는 새 테이블
     */
    public static List<String> targets(Schema schema) {
        List<String> targets = new ArrayList<>(SEEDED.stream().filter(schema::hasTable).toList());
        for (String table : schema.tableNames()) {
            if (!SEEDED.contains(table) && !RUNTIME.contains(table) && !KEPT.contains(table)) {
                targets.add(table);
            }
        }
        return targets;
    }

    /**
     * 자동 엔진이 손대지 않는 테이블(Seeder가 맡거나 일부러 비워 두는 것)
     */
    public static Set<String> known() {
        Set<String> known = new LinkedHashSet<>(SEEDED);
        known.addAll(RUNTIME);
        known.addAll(KEPT);
        return known;
    }
}
