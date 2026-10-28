package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetSummary;

/**
 * [반려동물 Mapper]
 * - PET·BRUP_REL 조회·저장 (반려동물 관리)
 */
@Mapper
public interface PetMapper {

    /**
     * 회원의 양육 반려동물 목록 조회
     */
    List<PetSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("coParentRoleCd") String coParentRoleCd);

    /**
     * 반려동물 단건 조회
     */
    Pet selectPet(@Param("petSn") Long petSn);

    /**
     * 품종명 조회
     */
    String selectVrtyNm(@Param("vrtySn") Long vrtySn);

    /**
     * 구분에 맞는 사용 중 품종 여부
     */
    boolean existsVrty(@Param("vrtySn") Long vrtySn, @Param("petSeCd") String petSeCd);

    /**
     * 회원의 양육관계 조회
     */
    BrupRel selectBrupRel(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn);

    /**
     * 반려동물의 양육자 목록 조회
     */
    List<CaregiverSummary> selectCaregivers(@Param("petSn") Long petSn);

    /**
     * 회원의 주양육 반려동물 수
     */
    int countPrimaryPets(@Param("mbrSn") Long mbrSn, @Param("primaryRoleCd") String primaryRoleCd);

    /**
     * 반려동물 등록
     */
    int insertPet(Pet pet);

    /**
     * 양육관계 등록
     */
    int insertBrupRel(BrupRel rel);

    /**
     * 반려동물 수정
     */
    int updatePet(@Param("pet") Pet pet, @Param("actorId") String actorId);

    /**
     * 회원의 대표 해제
     */
    int updateRepresentativeOff(@Param("mbrSn") Long mbrSn, @Param("actorId") String actorId);

    /**
     * 대표 지정
     */
    int updateRepresentativeOn(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);
}
