package com.nyangmeong.pet.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [케어미션]
 * - CARE_MSN 행을 MSN_NM(UK)으로 찾음 : 이름을 바꾸면 db/migration 과 같이 바꿈
 * - 케어리스트 미션은 건강기록 기본 항목(FN-HLT-003-12)만, 사용자 추가·처방약·반복 예방 항목은 미션 아님
 */
public enum CareMission {
    TOILET_CLEANING("화장실 청소하기", CareMissionPool.DAILY, Condition.ALWAYS),
    TOOTH_BRUSHING("양치하기", CareMissionPool.DAILY, Condition.ALWAYS),
    BRUSHING("빗질하기", CareMissionPool.DAILY, Condition.ALWAYS),
    PAW_CARE("발 관리하기", CareMissionPool.DAILY, Condition.ALWAYS),
    PLAY("놀이하기", CareMissionPool.DAILY, Condition.ALWAYS),
    WEIGHT_RECORD("체중 기록하기", CareMissionPool.DAILY, Condition.ALWAYS),
    MEAL_RECORD("식사량 기록하기", CareMissionPool.DAILY, Condition.ALWAYS),
    WATER_RECORD("음수량 기록하기", CareMissionPool.DAILY, Condition.ALWAYS),
    STOOL_RECORD("배변 기록하기", CareMissionPool.DAILY, Condition.ALWAYS),
    EXPENSE_CREATE("가계부 지출 등록하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    SCHEDULE_CREATE("케어 캘린더 일정 등록하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    EAR_CLEANING("귀 청소하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    NAIL_CARE("발톱 관리하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    BATH("목욕하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    EYE_CARE("눈 주변 관리하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    PAW_HAIR_CARE("발바닥 털 관리하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    MOUTH_CHECK("구강 상태 확인하기", CareMissionPool.ACTIVITY, Condition.ALWAYS),
    WALK_RECORD("산책 기록하기", CareMissionPool.ACTIVITY, Condition.DOG),
    OBSERVATION_RECORD("관찰 기록 추가하기", CareMissionPool.ACTIVITY, Condition.OBSERVING),
    PET_STORY_POST("펫이야기 글 등록하기", CareMissionPool.ACTIVITY, Condition.REPRESENTATIVE),
    PET_STORY_COMMENT("펫이야기 댓글 작성하기", CareMissionPool.ACTIVITY, Condition.REPRESENTATIVE);

    /**
     * [출제 조건]
     */
    public enum Condition {
        ALWAYS, DOG, OBSERVING, REPRESENTATIVE
    }

    private final String msnNm;
    private final CareMissionPool pool;
    private final Condition condition;

    CareMission(String msnNm, CareMissionPool pool, Condition condition) {
        this.msnNm = msnNm;
        this.pool = pool;
        this.condition = condition;
    }

    public String getMsnNm() {
        return msnNm;
    }

    public CareMissionPool getPool() {
        return pool;
    }

    /**
     * 미션명으로 케어미션 조회
     */
    public static Optional<CareMission> fromMsnNm(String msnNm) {
        return Arrays.stream(values()).filter(m -> m.msnNm.equals(msnNm)).findFirst();
    }

    /**
     * 출제 조건 충족 여부
     */
    public boolean canIssue(boolean dog, boolean observing, boolean representative) {
        return switch (condition) {
            case ALWAYS -> true;
            case DOG -> dog;
            case OBSERVING -> observing;
            case REPRESENTATIVE -> representative;
        };
    }
}
