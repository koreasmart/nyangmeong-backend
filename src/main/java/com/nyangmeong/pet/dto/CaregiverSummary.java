package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [양육자 목록 항목]
 * - PetResponse.caregivers, 주양육자에게 전원·공동양육자에게 본인 1건
 */
@Data
public class CaregiverSummary {
    private Long brupRelSn;
    private Long mbrSn;
    private String mbrNcnm;
    private String brupRoleCd;
}
