package com.nyangmeong.seed.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Seeder 사이에서 넘기는 값]
 * - 앞 Seeder가 만든 회원·반려동물 등을 뒤 Seeder가 고른다
 */
public final class World {

    public enum MemberStatus {
        ACTIVE, SUSPENDED, WITHDRAWN
    }

    public record Member(long sn, String email, String nickname, boolean admin, MemberStatus status,
            LocalDateTime joinedAt) {

        /** 화면에 활동 데이터가 있는 회원(정상 일반회원) */
        public boolean active() {
            return status == MemberStatus.ACTIVE && !admin;
        }
    }

    public record Pet(long sn, long ownerSn, boolean dog, String name, boolean deleted, Long vrtySn,
            LocalDate birth, BigDecimal weight, LocalDateTime registeredAt) {
    }

    /** 시군구 단계 지역 */
    public record Region(long sn, String ctpvNm, String sggNm, double lat, double lot) {
    }

    public record Post(long sn, long mbrSn, LocalDateTime at, boolean visible) {
    }

    public final List<Member> members = new ArrayList<>();
    public final List<Pet> pets = new ArrayList<>();
    public final List<Region> regions = new ArrayList<>();
    public final List<Long> dogVarieties = new ArrayList<>();
    public final List<Long> catVarieties = new ArrayList<>();
    public final Map<Long, String> varietyNames = new HashMap<>();
    public final List<Post> posts = new ArrayList<>();

    /** 회원 → 양육 중인(삭제 안 된) 반려동물 번호. 주양육·공동양육 모두 */
    public final Map<Long, List<Long>> caredPets = new HashMap<>();
    /** 회원 → 대표 반려동물 번호 */
    public final Map<Long, Long> representativePet = new HashMap<>();
    /** 픽셀 캐릭터 번호 → 반려동물 번호 */
    public final Map<Long, Long> characterPet = new HashMap<>();
    /** 픽셀 캐릭터 번호 → 현재 표시 중인 캐릭터 사진 번호 */
    public final Map<Long, Long> characterPhoto = new HashMap<>();
    /** 건강기록 출처 영수증(저장완료) 번호 → 회원 번호. 검진기록이 연결한다 */
    public final Map<Long, Long> healthReceipts = new HashMap<>();

    public List<Member> activeMembers() {
        return members.stream().filter(Member::active).toList();
    }

    public List<Member> admins() {
        return members.stream().filter(Member::admin).toList();
    }

    public Member member(long sn) {
        return members.get((int) sn - 1);
    }

    public Pet pet(long sn) {
        return pets.get((int) sn - 1);
    }

    public List<Pet> livePets() {
        return pets.stream().filter(p -> !p.deleted()).toList();
    }

    public List<Post> visiblePosts() {
        return posts.stream().filter(Post::visible).toList();
    }
}
