package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterJobStatusHistory;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.HeldItemSummary;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.dto.PixelCharacterSummary;

/**
 * [픽셀 캐릭터 Mapper]
 * - PET·BRUP_REL 은 조회만 (등록·수정은 반려동물 관리)
 */
@Mapper
public interface PixelCharacterMapper {

    /**
     * 회원의 양육 반려동물별 캐릭터 목록 조회
     */
    List<PixelCharacterSummary> selectList(Long mbrSn);

    /**
     * 캐릭터 조회
     */
    PixelCharacter selectPixelCharacter(Long pixelCharSn);

    /**
     * 반려동물의 캐릭터 조회
     */
    PixelCharacter selectByPetSn(Long petSn);

    /**
     * 캐릭터 등록
     */
    void insertPixelCharacter(PixelCharacter character);

    /**
     * 오류 확인 중(기본 캐릭터 대체) 여부 변경
     */
    int updateBscCharSbstYn(@Param("pixelCharSn") Long pixelCharSn, @Param("bscCharSbstYn") String bscCharSbstYn,
            @Param("actorId") String actorId);

    /**
     * 생성 작업 조회
     */
    CharacterCreationJob selectJob(Long charCrtJobSn);

    /**
     * 멱등 요청 ID로 생성 작업 조회
     */
    CharacterCreationJob selectJobByIdmptDmndId(String idmptDmndId);

    /**
     * 반려동물의 최근 생성 작업 조회
     */
    CharacterCreationJob selectLatestJob(Long petSn);

    /**
     * 생성 작업 등록
     */
    void insertJob(CharacterCreationJob job);

    /**
     * 생성 시작 처리
     */
    int updateJobStarted(@Param("charCrtJobSn") Long charCrtJobSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 생성 종료 처리
     */
    int updateJobFinished(@Param("charCrtJobSn") Long charCrtJobSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("failRsn") String failRsn);

    /**
     * 생성 작업에 오류 문의 연결
     */
    int updateJobInquiry(@Param("charCrtJobSn") Long charCrtJobSn, @Param("otoInqrySn") Long otoInqrySn);

    /**
     * 생성 작업 상태 이력 등록
     */
    void insertJobStatusHistory(CharacterJobStatusHistory history);

    /**
     * 캐릭터 사진 등록
     */
    void insertPhoto(CharacterPhoto photo);

    /**
     * 생성 작업의 사진 조회
     */
    CharacterPhoto selectJobPhoto(@Param("charCrtJobSn") Long charCrtJobSn, @Param("photoRoleCd") String photoRoleCd);

    /**
     * 현재 표시 중인 결과 사진 조회
     */
    CharacterPhoto selectCurrentPhoto(Long pixelCharSn);

    /**
     * 현재 표시 사진 해제
     */
    int updatePhotoHidden(Long pixelCharSn);

    /**
     * 보관함 아이템 목록 조회
     */
    List<HeldItemSummary> selectHeldItems(Long pixelCharSn);

    /**
     * 보유 아이템 전부 착용 해제
     */
    int updateUnequipAll(Long pixelCharSn);

    /**
     * 보유 아이템 착용
     */
    int updateEquip(@Param("pixelCharSn") Long pixelCharSn, @Param("hldItemSns") List<Long> hldItemSns);

    /**
     * 캐릭터 생성용 반려동물 정보 조회
     */
    PetProfileSummary selectPetProfile(Long petSn);
}
