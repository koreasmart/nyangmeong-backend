package com.nyangmeong.pet.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.domain.PetCodeGroup;
import com.nyangmeong.pet.domain.PetColor;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetCreateRequest;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.dto.PetUpdateRequest;
import com.nyangmeong.pet.mapper.PetMapper;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 서비스]
 * - 내 반려동물 목록 (SCR-MYP-001·002), 다른 도메인도 이 목록을 씀
 * - 등록·수정·상세·대표 지정 (SCR-MYP-003, FN-MYP-010-05)
 * - 정렬 = 내 양육관계 등록순 (docs/api/issues.md 마이페이지 1번 결정 전까지)
 */
@Service
@RequiredArgsConstructor
public class PetService {

    /** 계정당 주양육 반려동물 상한 */
    private static final int MAX_PET_COUNT = 10;

    private final PetMapper petMapper;
    private final PetAuthorizer petAuthorizer;
    private final CommonCodeService commonCodeService;

    /**
     * 내 반려동물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PetSummary> searchMyPets(Long mbrSn) {
        return petMapper.selectList(mbrSn, BrupRole.CO_PARENT.getCode());
    }

    /**
     * 반려동물 상세 조회
     */
    @Transactional(readOnly = true)
    public PetResponse getPet(Long mbrSn, Long petSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return toResponse(mbrSn, loadPet(petSn));
    }

    /**
     * 반려동물 등록
     */
    @Transactional
    public PetResponse createPet(Long mbrSn, PetCreateRequest req) {
        int primaryCount = petMapper.countPrimaryPets(mbrSn, BrupRole.PRIMARY.getCode());
        if (primaryCount >= MAX_PET_COUNT) {
            throw new CustomException(ErrorCode.PET_LIMIT_EXCEEDED);
        }

        Pet pet = new Pet();
        pet.setPetNm(req.petNm());
        pet.setPetSeCd(req.petSeCd());
        pet.setVrtySn(req.vrtySn());
        pet.setVrtyDrctInptNm(emptyToNull(req.vrtyDrctInptNm()));
        pet.setPetBrdt(req.petBrdt());
        pet.setChipNo(emptyToNull(req.chipNo()));
        pet.setPetGndrCd(req.petGndrCd());
        pet.setNutrYn(Boolean.TRUE.equals(req.nutrYn()) ? "Y" : "N");
        pet.setPetBdwg(req.petBdwg());
        pet.setGoalBdwg(req.goalBdwg());
        pet.setMinBdwg(req.minBdwg());
        pet.setMaxBdwg(req.maxBdwg());
        pet.setColrCd(req.colrCd());
        pet.setColrDrctInptNm(emptyToNull(req.colrDrctInptNm()));
        pet.setChrcCn(emptyToNull(req.chrcCn()));
        pet.setAlgyCn(normalizeList(req.algyCn()));
        pet.setDssCn(normalizeList(req.dssCn()));
        checkPet(pet);

        String actorId = String.valueOf(mbrSn);
        pet.setRgtrId(actorId);
        pet.setMdfrId(actorId);
        petMapper.insertPet(pet);

        BrupRel rel = new BrupRel();
        rel.setPetSn(pet.getPetSn());
        rel.setMbrSn(mbrSn);
        rel.setBrupRoleCd(BrupRole.PRIMARY.getCode());
        /** 첫 반려동물 = 대표 (FN-MYP-010-05) */
        rel.setRprsPetYn(primaryCount == 0 ? "Y" : "N");
        rel.setMdfrId(actorId);
        petMapper.insertBrupRel(rel);

        return toResponse(mbrSn, loadPet(pet.getPetSn()));
    }

    /**
     * 반려동물 수정
     */
    @Transactional
    public PetResponse updatePet(Long mbrSn, Long petSn, PetUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        Pet pet = loadPet(petSn);

        if (req.petNm() != null) {
            pet.setPetNm(req.petNm());
        }
        if (req.petSeCd() != null) {
            pet.setPetSeCd(req.petSeCd());
        }
        if (req.vrtySn() != null && hasText(req.vrtyDrctInptNm())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (req.vrtySn() != null) {
            pet.setVrtySn(req.vrtySn());
            pet.setVrtyDrctInptNm(null);
        }
        if (req.vrtyDrctInptNm() != null) {
            pet.setVrtyDrctInptNm(emptyToNull(req.vrtyDrctInptNm()));
            if (pet.getVrtyDrctInptNm() != null) {
                pet.setVrtySn(null);
            }
        }
        if (req.petBrdt() != null) {
            pet.setPetBrdt(req.petBrdt());
        }
        if (req.chipNo() != null) {
            pet.setChipNo(emptyToNull(req.chipNo()));
        }
        if (req.petGndrCd() != null) {
            pet.setPetGndrCd(req.petGndrCd());
        }
        if (req.nutrYn() != null) {
            pet.setNutrYn(req.nutrYn() ? "Y" : "N");
        }
        if (req.petBdwg() != null) {
            pet.setPetBdwg(req.petBdwg());
        }
        if (req.goalBdwg() != null) {
            pet.setGoalBdwg(req.goalBdwg());
        }
        if (req.minBdwg() != null) {
            pet.setMinBdwg(req.minBdwg());
        }
        if (req.maxBdwg() != null) {
            pet.setMaxBdwg(req.maxBdwg());
        }
        if (req.colrCd() != null) {
            pet.setColrCd(req.colrCd());
        }
        if (req.colrDrctInptNm() != null) {
            pet.setColrDrctInptNm(emptyToNull(req.colrDrctInptNm()));
        }
        if (req.chrcCn() != null) {
            pet.setChrcCn(emptyToNull(req.chrcCn()));
        }
        if (req.algyCn() != null) {
            pet.setAlgyCn(normalizeList(req.algyCn()));
        }
        if (req.dssCn() != null) {
            pet.setDssCn(normalizeList(req.dssCn()));
        }
        checkPet(pet);

        petMapper.updatePet(pet, String.valueOf(mbrSn));
        return toResponse(mbrSn, loadPet(petSn));
    }

    /**
     * 대표 반려동물 지정
     */
    @Transactional
    public void designateRepresentative(Long mbrSn, Long petSn) {
        /** 공동양육 동물은 대표 불가 (FN-MYP-010-05) */
        petAuthorizer.checkPrimary(mbrSn, petSn);
        String actorId = String.valueOf(mbrSn);
        petMapper.updateRepresentativeOff(mbrSn, actorId);
        petMapper.updateRepresentativeOn(mbrSn, petSn, actorId);
    }

    /**
     * 삭제되지 않은 반려동물 조회
     */
    private Pet loadPet(Long petSn) {
        Pet pet = petMapper.selectPet(petSn);
        if (pet == null) {
            throw new CustomException(ErrorCode.PET_NOT_FOUND);
        }
        return pet;
    }

    /**
     * 상세 응답 조립
     */
    private PetResponse toResponse(Long mbrSn, Pet pet) {
        BrupRel rel = petMapper.selectBrupRel(mbrSn, pet.getPetSn());
        List<CaregiverSummary> caregivers = petMapper.selectCaregivers(pet.getPetSn());
        if (!BrupRole.PRIMARY.is(rel.getBrupRoleCd())) {
            caregivers = caregivers.stream().filter(c -> mbrSn.equals(c.getMbrSn())).toList();
        }
        String vrtyNm = pet.getVrtySn() != null ? petMapper.selectVrtyNm(pet.getVrtySn()) : null;
        return PetResponse.from(pet, vrtyNm, rel, caregivers);
    }

    /**
     * 저장 직전 값 검사
     */
    private void checkPet(Pet pet) {
        checkCode(PetCodeGroup.PET_SE, pet.getPetSeCd());
        checkCode(PetCodeGroup.GENDER, pet.getPetGndrCd());
        checkCode(PetCodeGroup.COLOR, pet.getColrCd());

        if ((pet.getVrtySn() == null) == (pet.getVrtyDrctInptNm() == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (pet.getVrtySn() != null && !petMapper.existsVrty(pet.getVrtySn(), pet.getPetSeCd())) {
            throw new CustomException(ErrorCode.BREED_NOT_FOUND);
        }

        if (PetColor.DIRECT_INPUT.is(pet.getColrCd())) {
            if (pet.getColrDrctInptNm() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else {
            pet.setColrDrctInptNm(null);
        }

        if (pet.getMinBdwg().compareTo(pet.getMaxBdwg()) >= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 공통코드 그룹 소속 검사
     */
    private void checkCode(PetCodeGroup group, String code) {
        boolean exists = commonCodeService.search(group.getCode()).stream()
                .map(CommonCodeSummary::getComCd)
                .anyMatch(c -> c.equals(code));
        if (!exists) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 쉼표 목록 정리
     */
    private String normalizeList(String value) {
        if (value == null) {
            return null;
        }
        String joined = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    /**
     * 빈 문자열 비움 처리
     */
    private String emptyToNull(String value) {
        return hasText(value) ? value : null;
    }

    /**
     * 값 있음 여부
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
