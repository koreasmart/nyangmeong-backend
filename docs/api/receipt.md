# 영수증(expense 도메인) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

영수증 API는 `expense` 도메인에 구현됐다(2026-10-22 구현에 맞춤). 정본은 Swagger다. 가계부 화면 쪽 영수증 저장을 누가 하는지는 `docs/OPEN_ISSUES.md` 영수증 행에서 정한다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| POST | /receipts | 영수증 이미지 업로드(multipart, 확장자·파일 시그니처 검증) → 요청 안에서 동기 OCR(제한 시간 `app.ocr.timeout`, 기본 15초) → 상태·추출 결과. 인식 실패·일시 오류도 201이고 `ocrPrcsSttsCd`·`failRsn`으로 구분. 오류: `FILE_TYPE_NOT_ALLOWED`·`FILE_TOO_LARGE`(400) | AIM-008-04~06·09, AIM-009-04·16·20·21, AIM-010-01·05 | LJS | ReceiptCreateRequest | ReceiptResponse |
| GET | /receipts/{recptSn}/image | 영수증 원본 이미지. 남의·없는 영수증은 404 `RECEIPT_NOT_FOUND` | AIM-009-01 | LJS | - | 이미지 파일 |
| POST | /receipts/{recptSn}/analyze | 다시 분석(취소·인식 실패·일시 오류 뒤). 200. 오류: `RECEIPT_ANALYZING`·`ALREADY_PROCESSED`(409) | AIM-008-07, AIM-010-05 | LJS | ReceiptAnalyzeRequest | ReceiptResponse |
| POST | /receipts/{recptSn}/cancel | 분석 취소(영수증 상태 '취소'). 오류: `RECEIPT_ALREADY_CANCELED`(409) | AIM-008-07 | LJS | - | - |
| POST | /receipts/{recptSn}/save-check | 저장 전 확인. 가계부 중복 후보와 검진일 앞뒤 7일의 미연결 병원 일정. 저장하지 않는다 | AIM-009-22·23, HLT-002-46 | LJS | ReceiptSaveCheckRequest | ReceiptSaveCheckResponse |
| POST | /receipts/{recptSn}/save | 확인 후 저장. 반려동물별 검진기록·병원 일정 연결/생성·처방약·가계부 지출을 한 트랜잭션으로, 영수증 상태 저장완료. 처음 201, 같은 `idmptDmndId` 재전송은 200과 같은 응답. 오류: `RECEIPT_NOT_SAVABLE`·`RECEIPT_ALREADY_SAVED`·`RECEIPT_ALREADY_CANCELED`·`RECEIPT_ANALYZING`·`ALREADY_PROCESSED`(409), `RECEIPT_NOT_FOUND`(404) | AIM-009-07~11·24·26·27, AIM-010-06, HLT-002-31·35·37~41·45 | LJS | ReceiptSaveRequest | ReceiptSaveResponse |

## DTO

### ReceiptAnalyzeRequest
쓰는 API: POST /receipts/{recptSn}/analyze
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | 진입한 반려동물. 로그인 회원과 연동된 반려동물 | PET.PET_SN, AIM-009-17 |

### ReceiptCreateRequest
쓰는 API: POST /receipts (multipart/form-data)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| file | MultipartFile | Y | jpg·jpeg·png·webp, 1건당 10MB 이하, 확장자·실제 형식 이중 검증. PDF 불가 | RECPT.ATCH_FILE_SN (AIM-008-05) |
| petSn | Long | Y | 진입한 반려동물. 로그인 회원과 연동된 반려동물 | PET.PET_SN, AIM-009-17 |

### ReceiptResponse
쓰는 API: POST /receipts; POST /receipts/{recptSn}/analyze
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| recptSn | Long | Y | | RECPT.RECPT_SN |
| ocrPrcsSttsCd | String | Y | AIMG01 | RECPT.OCR_PRCS_STTS_CD (인식 실패·일시 오류 구분, AIM-010-01) |
| failRsn | String | N | 일시 오류 사유 `TIMEOUT`·`API_ERROR`(화면 문구 2종) | RECPT_STTS_HSTRY.CHG_RSN, AIM-010-07 |
| hsptlNm | String | N | 추출 병원명 | AIM-009-02 |
| ckupYmd | LocalDate | N | 추출 검진일 | AIM-009-02 |
| recptAmt | Long | N | 영수증 최종 결제 금액 | AIM-009-02 |
| dstrbDscntAmt | Long | N | 구역에 나눠 준 하단 할인 합계 | AIM-009-20, HLT-002-42 |
| sections | List<Section> | N | 반려동물 구역별 구역명·진료 품목·금액·약품 행 | AIM-009-16 |

#### Section (ReceiptResponse.sections)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| sectionNm | String | N | 구역의 반려동물 이름(예: 미르). 구역 없는 영수증은 null인 구역 1개 | AIM-009-16·17 |
| petSn | Long | N | 구역 이름이 회원의 반려동물 이름과 공백 제외 일치하면 자동 연결, 못 하면 null | PET.PET_SN, AIM-009-17 |
| sbttlAmt | Long | N | 구역 소계 | HLT-002-39, AIM-009-20 |
| dscntAmt | Long | N | 구역 할인(하단 할인을 비율 배분한 몫 포함) | AIM-009-20, HLT-002-42 |
| sectionAmt | Long | N | `sbttlAmt - dscntAmt` | AIM-009-20 |
| ckupCn | String | N | 진료 품목으로 채운 검진 내용 | AIM-009-21, HLT-002-38 |
| items | List<Line> | Y | 진료 품목(약품 제외). 0건이면 빈 목록. `Line`은 `itemNm`(String)·`itemAmt`(Long) | AIM-009-19·21 |
| prescriptions | List<Prescription> | Y | 약품 행. 0건이면 빈 목록. `Prescription`은 `mdcnNm`·`mdcnBgngYmd`·`mdcnEndYmd`(검진일 + 일수 − 1)·`dayMdcnNmtm`·`prscSttsCd`(HLTG06)·`mdcnAmt`(대조용, 저장 안 함), 인식 실패 칸은 null | AIM-009-03·04 |

### ReceiptSaveCheckRequest
쓰는 API: POST /receipts/{recptSn}/save-check
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupYmd | LocalDate | Y | 검진일 | AIM-009-07 |
| pets | List<Pet> | Y | 1건 이상. `Pet`은 `petSn`(Long, 필수)·`amt`(Long, 0 이상)·`expenseYn`(Boolean) | AIM-009-22·23 |

### ReceiptSaveCheckResponse
쓰는 API: POST /receipts/{recptSn}/save-check
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| duplicateExpenses | List<DuplicateExpense> | Y | 같은 날·같은 금액·병원 카테고리·같은 반려동물 지출. `petSn`·`expndSn`·`expndYmd`·`expndAmt`·`dtlMttr`·`expndPlcusNm`. 목록은 "내용 · 사용처"로 표시(빈 값 생략, MYP-031-06). [아니오]를 고른 탭은 저장 때 `expenseYn=false`로 보낸다 | AIM-009-22·23 |
| nearbySchedules | List<NearbySchedule> | Y | 검진일 앞뒤 7일의 검진기록 미연결 병원 일정. `petSn`·`schdlSn`·`schdlDt`·`hsptlNm`. [옮겨 연결하기]를 고르면 저장 때 `schdlSn`으로 보낸다 | HLT-002-46 |

### ReceiptSaveRequest
쓰는 API: POST /receipts/{recptSn}/save
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| idmptDmndId | String | Y | 최대 100자. 중복 클릭·재전송 차단 | RECPT.IDMPT_DMND_ID (AIM-009-24) |
| hsptlNm | String | Y | 1~50자(AIM-009-26. 컬럼은 80자, issues 11) | CKUP_RCD.HSPTL_NM (AIM-009-07) |
| ckupYmd | LocalDate | Y | 모든 반려동물에 공통 | AIM-009-07 |
| expndPlcusNm | String | N | 가계부 사용처. 최대 80자, 모든 탭에 공통. 모달이 병원명을 기본값으로 채운다. 비우면(공백 포함) 사용처 없이 기록하고, 세부사항(`DTL_MTTR`)은 늘 비운다 | EXPND.EXPND_PLCUS_NM (`db/migration/20261025_01_add_expnd_plcus_nm.sql`, AIM-009-10) |
| pets | List<ReceiptSavePetRequest> | Y | 1건 이상('저장 안 함' 탭은 보내지 않음) | AIM-009-09 |

#### ReceiptSavePetRequest (ReceiptSaveRequest.pets)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | 로그인 회원과 연동된 반려동물 | PET.PET_SN |
| ckupTypeCd | String | Y | HLTG01 공통코드 | CKUP_RCD.CKUP_TYPE_CD |
| ckupCn | String | N | 최대 500자 | CKUP_RCD.CKUP_CN (AIM-009-21) |
| ckupMemoCn | String | N | 최대 200자 | CKUP_RCD.CKUP_MEMO_CN |
| amt | Long | N | 0 이상. `expenseYn`이고 1원 이상이면 지출을 만든다 | EXPND.EXPND_AMT |
| expenseYn | Boolean | N | 가계부 기록 여부. 중복 확인에서 [아니오]한 탭은 false | AIM-009-22·23 |
| schdlSn | Long | N | [옮겨 연결하기]를 고른 반려동물만 | CKUP_RCD.SCHDL_SN (HLT-002-46) |
| drugs | List<ReceiptSaveDrugRequest> | N | 0건 허용. 주기 체크 OFF(`CARE_RFLT_YN='N'`)로 저장 | AIM-009-03 |

#### ReceiptSaveDrugRequest (ReceiptSavePetRequest.drugs)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mdcnNm | String | Y | 1~40자(AIM-009-26) | PRSC.MDCN_NM |
| mdcnBgngYmd | LocalDate | Y | | PRSC.MDCN_BGNG_YMD |
| mdcnEndYmd | LocalDate | Y | 시작일 이후(같은 날 허용, AIM-009-07) | PRSC.MDCN_END_YMD |
| dayMdcnNmtm | Integer | Y | 1 이상 | PRSC.DAY_MDCN_NMTM |
| prscSttsCd | String | Y | HLTG06 공통코드 | PRSC.PRSC_STTS_CD |

### ReceiptSaveResponse
쓰는 API: POST /receipts/{recptSn}/save
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| recptSn | Long | Y | | RECPT.RECPT_SN |
| ckupRcds | List<CheckupRecord> | Y | 만든 검진기록. `petSn`·`ckupRcdSn` | CKUP_RCD.CKUP_RCD_SN |
