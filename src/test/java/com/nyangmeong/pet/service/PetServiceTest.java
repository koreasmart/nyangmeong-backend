package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetCreateRequest;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.dto.PetUpdateRequest;
import com.nyangmeong.pet.mapper.PetMapper;

/**
 * [검사 - 내 반려동물 목록·등록·수정·대표 지정]
 */
class PetServiceTest {

    private static final Long MBR = 1L;

    private static final Long PET = 100L;
    private static final String DOG = "PETC0001";
    private static final String CAT = "PETC0002";
    private static final String MALE = "PETC0003";
    private static final String WHITE = "PETC0006";
    private static final String DIRECT_COLOR = "PETC0017";

    private final PetMapper mapper = mock(PetMapper.class);
    private final PetAuthorizer authorizer = mock(PetAuthorizer.class);
    private final CommonCodeService codes = mock(CommonCodeService.class);
    private final PetService service = new PetService(mapper, authorizer, codes);

    @BeforeEach
    void setUp() {
        when(codes.search("PETG01")).thenReturn(codeList(DOG, CAT));
        when(codes.search("PETG02")).thenReturn(codeList(MALE, "PETC0004"));
        when(codes.search("PETG03")).thenReturn(codeList(WHITE, DIRECT_COLOR));
        when(mapper.existsVrty(15L, DOG)).thenReturn(true);
        when(mapper.selectVrtyNm(15L)).thenReturn("푸들");
        doAnswer(inv -> {
            inv.<Pet>getArgument(0).setPetSn(PET);
            return 1;
        }).when(mapper).insertPet(any());
        when(mapper.selectPet(PET)).thenAnswer(inv -> storedPet());
        when(mapper.selectBrupRel(anyLong(), eq(PET))).thenReturn(rel(BrupRole.PRIMARY.getCode(), "N"));
        when(mapper.selectCaregivers(PET)).thenReturn(new ArrayList<>());
    }

    @Test
    void 목록은_공동양육자_코드로_인원을_센_결과를_그대로_돌려준다() {
        PetSummary primary = pet(10L, BrupRole.PRIMARY.getCode(), true, 2, "410000000000001");
        PetSummary coParent = pet(11L, BrupRole.CO_PARENT.getCode(), false, 1, null);
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of(primary, coParent));

        List<PetSummary> result = service.searchMyPets(MBR);

        assertThat(result).extracting(PetSummary::getPetSn).containsExactly(10L, 11L);
        assertThat(result.get(0).isRprsPetYn()).isTrue();
        assertThat(result.get(0).getCaregiverCount()).isEqualTo(2);
        assertThat(result.get(1).getBrupRoleCd()).isEqualTo(BrupRole.CO_PARENT.getCode());
    }

    @Test
    void 칩이_없는_반려동물의_chipNo는_null() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode()))
                .thenReturn(List.of(pet(10L, BrupRole.PRIMARY.getCode(), true, 0, null)));

        assertThat(service.searchMyPets(MBR).get(0).getChipNo()).isNull();
    }

    @Test
    void 양육_반려동물이_없으면_빈_목록() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of());

        assertThat(service.searchMyPets(MBR)).isEmpty();
    }

    private PetSummary pet(Long petSn, String roleCd, boolean rprs, int caregivers, String chipNo) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setBrupRoleCd(roleCd);
        pet.setRprsPetYn(rprs);
        pet.setCaregiverCount(caregivers);
        pet.setChipNo(chipNo);
        return pet;
    }

    @Test
    void 등록은_PET과_주양육_양육관계를_저장한다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(2);

        PetResponse res = service.createPet(MBR, create(15L, null, " 새우, ,닭고기 ", " , "));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).insertPet(pet.capture());
        assertThat(pet.getValue().getAlgyCn()).isEqualTo("새우,닭고기");
        assertThat(pet.getValue().getDssCn()).isNull();
        assertThat(pet.getValue().getRgtrId()).isEqualTo("1");
        ArgumentCaptor<BrupRel> rel = ArgumentCaptor.forClass(BrupRel.class);
        verify(mapper).insertBrupRel(rel.capture());
        assertThat(rel.getValue().getPetSn()).isEqualTo(PET);
        assertThat(rel.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(rel.getValue().getBrupRoleCd()).isEqualTo(BrupRole.PRIMARY.getCode());
        assertThat(rel.getValue().getRprsPetYn()).isEqualTo("N");
        assertThat(res.petSn()).isEqualTo(PET);
    }

    @Test
    void 주양육_0마리에서_등록하면_대표가_된다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(0);

        service.createPet(MBR, create(15L, null, null, null));

        ArgumentCaptor<BrupRel> rel = ArgumentCaptor.forClass(BrupRel.class);
        verify(mapper).insertBrupRel(rel.capture());
        assertThat(rel.getValue().getRprsPetYn()).isEqualTo("Y");
    }

    @Test
    void 주양육_10마리면_등록이_막힌다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(10);

        assertCode(() -> service.createPet(MBR, create(15L, null, null, null)), ErrorCode.PET_LIMIT_EXCEEDED);
        verify(mapper, never()).insertPet(any());
    }

    @Test
    void 주양육_9마리면_공동양육과_상관없이_등록된다() {
        when(mapper.countPrimaryPets(MBR, BrupRole.PRIMARY.getCode())).thenReturn(9);

        service.createPet(MBR, create(15L, null, null, null));

        verify(mapper).insertPet(any());
    }

    @Test
    void 품종을_둘_다_보내거나_둘_다_없으면_400() {
        assertCode(() -> service.createPet(MBR, create(15L, "믹스", null, null)), ErrorCode.INVALID_INPUT_VALUE);
        assertCode(() -> service.createPet(MBR, create(null, null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다른_구분의_품종이면_BREED_NOT_FOUND() {
        when(mapper.existsVrty(30L, DOG)).thenReturn(false);

        assertCode(() -> service.createPet(MBR, create(30L, null, null, null)), ErrorCode.BREED_NOT_FOUND);
    }

    @Test
    void 없는_털색_코드면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("3"), kg("5"), "PETC9999", null, null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 털색_직접_입력인데_이름이_없으면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("3"), kg("5"), DIRECT_COLOR, " ", null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 최소_체중이_최대_이상이면_400() {
        PetCreateRequest req = new PetCreateRequest("뭉치", DOG, 15L, null, LocalDate.of(2023, 5, 1), null, MALE,
                true, kg("4.2"), null, kg("5"), kg("5"), WHITE, null, null, null, null);

        assertCode(() -> service.createPet(MBR, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 공동양육자도_수정할_수_있고_보낸_필드만_바뀐다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(BrupRole.CO_PARENT.getCode(), "N"));

        service.updatePet(MBR, PET, update(null, kg("6.5"), false, null, null));

        verify(authorizer).checkCaregiver(MBR, PET);
        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getPetBdwg()).isEqualByComparingTo("6.5");
        assertThat(pet.getValue().getNutrYn()).isEqualTo("N");
        assertThat(pet.getValue().getPetNm()).isEqualTo("뭉치");
        assertThat(pet.getValue().getMaxBdwg()).isEqualByComparingTo("5");
    }

    @Test
    void 양육자가_아니면_수정이_막힌다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(authorizer).checkCaregiver(MBR, PET);

        assertCode(() -> service.updatePet(MBR, PET, update(null, kg("6.5"), null, null, null)),
                ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_최소만_보내_기존_최대_이상이면_400() {
        assertCode(() -> service.updatePet(MBR, PET, update(kg("5"), null, null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updatePet(any(), anyString());
    }

    @Test
    void 수정에서_알레르기를_정리해_저장하고_빈_목록은_비운다() {
        service.updatePet(MBR, PET, update(null, null, null, " 새우, ,닭고기 ", " , "));

        ArgumentCaptor<Pet> pet = ArgumentCaptor.forClass(Pet.class);
        verify(mapper).updatePet(pet.capture(), eq("1"));
        assertThat(pet.getValue().getAlgyCn()).isEqualTo("새우,닭고기");
        assertThat(pet.getValue().getDssCn()).isNull();
    }

    @Test
    void 상세는_공동양육자에게_본인_양육관계만_준다() {
        when(mapper.selectBrupRel(MBR, PET)).thenReturn(rel(BrupRole.CO_PARENT.getCode(), "N"));
        when(mapper.selectCaregivers(PET)).thenReturn(List.of(caregiver(9L, BrupRole.PRIMARY.getCode()),
                caregiver(MBR, BrupRole.CO_PARENT.getCode())));

        PetResponse res = service.getPet(MBR, PET);

        assertThat(res.caregivers()).extracting(CaregiverSummary::getMbrSn).containsExactly(MBR);
        assertThat(res.vrtyNm()).isEqualTo("푸들");
    }

    @Test
    void 대표_지정은_기존_대표를_해제한_뒤_지정한다() {
        service.designateRepresentative(MBR, PET);

        InOrder order = inOrder(authorizer, mapper);
        order.verify(authorizer).checkPrimary(MBR, PET);
        order.verify(mapper).updateRepresentativeOff(MBR, "1");
        order.verify(mapper).updateRepresentativeOn(MBR, PET, "1");
    }

    @Test
    void 공동양육_동물은_대표로_지정할_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(authorizer).checkPrimary(MBR, PET);

        assertCode(() -> service.designateRepresentative(MBR, PET), ErrorCode.PET_PRIMARY_ONLY);
        verify(mapper, never()).updateRepresentativeOff(anyLong(), anyString());
    }

    private void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(code));
    }

    private PetCreateRequest create(Long vrtySn, String vrtyDrctInptNm, String algyCn, String dssCn) {
        return new PetCreateRequest("뭉치", DOG, vrtySn, vrtyDrctInptNm, LocalDate.of(2023, 5, 1), "", MALE, true,
                kg("4.2"), null, kg("3"), kg("5"), WHITE, null, null, algyCn, dssCn);
    }

    private PetUpdateRequest update(BigDecimal minBdwg, BigDecimal petBdwg, Boolean nutrYn, String algyCn,
            String dssCn) {
        return new PetUpdateRequest(null, null, null, null, null, null, null, nutrYn, petBdwg, null, minBdwg, null,
                null, null, null, algyCn, dssCn);
    }

    private Pet storedPet() {
        Pet pet = new Pet();
        pet.setPetSn(PET);
        pet.setPetNm("뭉치");
        pet.setPetSeCd(DOG);
        pet.setVrtySn(15L);
        pet.setPetGndrCd(MALE);
        pet.setPetBrdt(LocalDate.of(2023, 5, 1));
        pet.setPetBdwg(kg("4.2"));
        pet.setMinBdwg(kg("3"));
        pet.setMaxBdwg(kg("5"));
        pet.setColrCd(WHITE);
        pet.setNutrYn("Y");
        return pet;
    }

    private BrupRel rel(String roleCd, String rprsPetYn) {
        BrupRel rel = new BrupRel();
        rel.setBrupRoleCd(roleCd);
        rel.setRprsPetYn(rprsPetYn);
        return rel;
    }

    private CaregiverSummary caregiver(Long mbrSn, String roleCd) {
        CaregiverSummary caregiver = new CaregiverSummary();
        caregiver.setMbrSn(mbrSn);
        caregiver.setBrupRoleCd(roleCd);
        return caregiver;
    }

    private List<CommonCodeSummary> codeList(String... comCds) {
        List<CommonCodeSummary> list = new ArrayList<>();
        for (String comCd : comCds) {
            CommonCodeSummary code = new CommonCodeSummary();
            code.setComCd(comCd);
            list.add(code);
        }
        return list;
    }

    private BigDecimal kg(String value) {
        return new BigDecimal(value);
    }
}
