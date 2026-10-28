# 건강기록(health) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /pets/{petSn}/care-completions | 케어 완료 기록 목록(기록 내용). `?careArtclSn=&fromYmd=&toYmd=` | HLT-003-12 | JAJ | - | List<CareCompletionSummary> |
| GET | /pets/{petSn}/care-items | 케어 항목 목록(토탈 케어리스트, 오늘 투약 횟수 포함). `?mainExpsrYn=&careArtclNm=` | HLT-003-12·13, AIM-012-08 | JAJ | - | List<CareItemSummary> |
| GET | /pets/{petSn}/care-schedules | 케어 캘린더 일정 목록. `?fromYmd=&toYmd=&schdlKndCd=&ckupTypeCd=&size=` — 월간·날짜 상세·다가오는 일정·다음 예정일·가까운 예정 일정 후보 | HLT-003-29~31·39·40, HLT-011-04, HLT-002-46 | JAJ | - | List<CareScheduleSummary> |
| GET | /pets/{petSn}/checkup-attachments | 검진기록 첨부 목록(최신순). `?ckupRcdSn=&atchTypeCd=&page=&size=` | HLT-005-01·02, HLT-011-09 | JAJ | - | Page<CheckupAttachmentSummary> |
| GET | /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn} | 첨부 파일 다운로드·이미지 미리보기(보호 파일, 파일 자체 반환) | HLT-005-01, HLT-011-09 | JAJ | - | 파일 |
| GET | /pets/{petSn}/checkups | 검진기록 목록(최신순). `?year=&ckupTypeCd=&page=&size=` — 리스트·필터 칩 건수·타임라인·예방/정기검진 이력 | HLT-002-05·06·14, HLT-011-03·04 | JAJ | - | Page<CheckupSummary> |
| GET | /pets/{petSn}/checkups/{ckupRcdSn} | 검진상세 기본정보 | HLT-005-01·15 | JAJ | - | CheckupResponse |
| GET | /pets/{petSn}/daily-health-records | 일상 건강기록 목록. `?dailyRcdIdctCd=&fromYmd=&toYmd=&size=` — 그래프(최근 14개)·오늘의 기록·날짜별 표·최신 체중 | HLT-002-09·10·12·13, HLT-011-02 | JAJ | - | List<DailyHealthRecordSummary> |
| GET | /pets/{petSn}/etc-inspections | 기타검사 목록. `?ckupRcdSn=` | HLT-005-02 | JAJ | - | List<EtcInspectionSummary> |
| GET | /pets/{petSn}/inspection-values | 혈액검사 수치 목록. `?ckupRcdSn=&inspArtclCd=` — 검진상세 결과·최근 검사·수치 추이 | HLT-005-02, HLT-011-02·06·07 | JAJ | - | List<InspectionValueSummary> |
| GET | /pets/{petSn}/observation-cycles | 관찰 회차 목록(관찰일 순). `?obsrvArtclSn=` | HLT-002-17~19 | JAJ | - | List<ObservationCycleSummary> |
| GET | /pets/{petSn}/observation-cycles/{obsrvCyclSn}/photo | 관찰 사진 원본·썸네일(보호 파일, 파일 자체 반환) | HLT-002-17·19, HLT-011-10 | JAJ | - | 파일 |
| GET | /pets/{petSn}/observation-items | 관찰 항목 목록(첫·최근 회차 요약 포함). `?obsrvSttsCd=` | HLT-002-15, HLT-011-10 | JAJ | - | List<ObservationItemSummary> |
| GET | /pets/{petSn}/prescriptions | 처방약 목록(복용 중 우선·최신순). `?ckupRcdSn=&prscSttsCd=` | HLT-005-03, HLT-011-02·08 | JAJ | - | List<PrescriptionSummary> |
| GET | /pets/{petSn}/repeat-preventions | 반복 예방 관리 목록. `?ckupRcdSn=` | HLT-005-05 | JAJ | - | List<RepeatPreventionSummary> |
| POST | /pets/{petSn}/care-completions | 케어 완료 체크(투약 항목은 1회 체크). 다음 예정일 재계산. 중복 체크는 `UK_CARE_CMPTN_HSTRY_1`(항목+완료일)로 막는다. 같은 항목·같은 날 다시 보내면 200과 기존 기록(처음은 201), `idmptDmndId`는 받지 않는다 | HLT-003-12 | JAJ | CareCompletionCreateRequest | CareCompletionResponse |
| POST | /pets/{petSn}/care-items | 케어 항목 추가(마지막으로 한 날은 완료 기록으로 함께 저장). AI 대화 등록도 같은 API | HLT-003-21~24·38, AIM-012-07·09·13 | JAJ | CareItemCreateRequest | CareItemResponse |
| POST | /pets/{petSn}/care-schedules | 일정 등록(종류별 필수값 서버 재검증, 출처 SCHDL_SRC_CD). AI 대화 등록도 같은 API | HLT-003-32~37, AIM-012-09·12~14 | JAJ | CareScheduleCreateRequest | CareScheduleResponse |
| POST | /pets/{petSn}/checkup-attachments | 첨부 등록(multipart, 유형·영상 종류·촬영 부위) | HLT-005-01 | JAJ | CheckupAttachmentCreateRequest | CheckupAttachmentResponse |
| POST | /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn}/analyses | 검사결과지 PDF 분석 요청(인식 결과만 반환, 저장 안 함, 중복 요청 차단). 요청 안에서 동기로 분석하고 제한 시간(설정값, 영수증 OCR의 `app.ocr.timeout`과 같은 방식)을 넘기면 일시 오류로 돌려준다. 성공·실패 모두 200, 결과는 `anlysRslt`로 구분(JAJ 확인) | HLT-005-04, HLT-007-06 | JAJ | - | InspectionAnalysisResponse |
| POST | /pets/{petSn}/checkups | 검진기록 직접 등록(같은 날짜 병원 일정 자동 연결·없으면 생성, 가까운 일정 옮겨 연결 선택) | HLT-002-26~29·31·46 | JAJ | CheckupCreateRequest | CheckupResponse |
| POST | /pets/{petSn}/daily-health-records | 오늘 기록 추가(식사량·음수량·산책·체중·배변상태) | HLT-002-11 | JAJ | DailyHealthRecordCreateRequest | DailyHealthRecordResponse |
| POST | /pets/{petSn}/etc-inspections | 기타검사 입력(검사명·결과 500자·메모 200자) | HLT-005-11~14 | JAJ | EtcInspectionCreateRequest | EtcInspectionResponse |
| POST | /pets/{petSn}/inspection-values | 검사 수치 여러 행 저장(혈액검사 고정, 이미 등록된 항목 제외, 삭제된 같은 항목은 복구) | HLT-007-02~05 | JAJ | InspectionValueCreateRequest | List<InspectionValueResponse> |
| POST | /pets/{petSn}/observation-cycles | 관찰 회차 추가(multipart 사진, 종료 항목 불가) | HLT-002-20~25·32 | JAJ | ObservationCycleCreateRequest | ObservationCycleResponse |
| POST | /pets/{petSn}/observation-items | 관찰 항목 등록(항목 + 1회차, multipart 사진) | HLT-002-20~25·32 | JAJ | ObservationItemCreateRequest | ObservationItemResponse |
| POST | /pets/{petSn}/observation-items/{obsrvArtclSn}/end | 관찰 종료(상태 전이, 기록 유지) | HLT-002-16 | JAJ | - | - |
| POST | /pets/{petSn}/prescriptions | 처방약 추가(주기 체크 ON이면 투약 케어 항목 생성) | HLT-005-03 | JAJ | PrescriptionCreateRequest | PrescriptionResponse |
| POST | /pets/{petSn}/repeat-preventions | 반복 예방 등록(정기관리 케어 항목 자동 추가) | HLT-005-05 | JAJ | RepeatPreventionCreateRequest | RepeatPreventionResponse |
| PUT | /pets/{petSn}/care-item-exposures | 메인 표시(★) 항목과 순서 전체 저장(MAIN_EXPSR_YN·MAIN_SORT_SEQ, 최대 6). 메인페이지 MAN-009도 같은 API | HLT-003-13 | JAJ | CareItemExposureSaveRequest | List<CareItemSummary> |
| PATCH | /pets/{petSn}/care-items/{careArtclSn} | 케어 항목 수정(사용자 항목 전체, 기본 정기관리 항목은 주기만) | HLT-003-12 | JAJ | CareItemUpdateRequest | CareItemResponse |
| PATCH | /pets/{petSn}/care-schedules/{schdlSn} | 일정 수정(반려동물 변경 시 연결 검진기록·자동 생성 케어 항목 이동, ★ 해제) | HLT-003-32~37 | JAJ | CareScheduleUpdateRequest | CareScheduleResponse |
| PATCH | /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn} | 첨부 1건 파일 교체·유형 수정(multipart) | HLT-005-01 | JAJ | CheckupAttachmentUpdateRequest | CheckupAttachmentResponse |
| PATCH | /pets/{petSn}/checkups/{ckupRcdSn} | 검진기록 수정(내용·메모 인라인, 검진일·반려동물 변경 시 일정 재연결·케어 항목 이동) | HLT-002-26~29, HLT-005-01 | JAJ | CheckupUpdateRequest | CheckupResponse |
| PATCH | /pets/{petSn}/daily-health-records/{dailyHlthRcdSn} | 일상 기록 인라인 수정(시각·값) | HLT-002-13 | JAJ | DailyHealthRecordUpdateRequest | DailyHealthRecordResponse |
| PATCH | /pets/{petSn}/etc-inspections/{etcInspSn} | 기타검사 수정 | HLT-005-02·11~14 | JAJ | EtcInspectionUpdateRequest | EtcInspectionResponse |
| PATCH | /pets/{petSn}/inspection-values/{inspNvlSn} | 검사 수치 1행 수정(항목 변경 불가) | HLT-007-02·03 | JAJ | InspectionValueUpdateRequest | InspectionValueResponse |
| PATCH | /pets/{petSn}/observation-cycles/{obsrvCyclSn} | 관찰 회차 수정(multipart) | HLT-002-19 | JAJ | ObservationCycleUpdateRequest | ObservationCycleResponse |
| PATCH | /pets/{petSn}/prescriptions/{prscSn} | 처방약 수정(투약 항목 목표 횟수·종료일 연동, 종료·OFF면 투약 항목 삭제) | HLT-005-03 | JAJ | PrescriptionUpdateRequest | PrescriptionResponse |
| DELETE | /pets/{petSn}/care-completions/{careCmptnHstrySn} | 완료 기록 취소·투약 마지막 1회 취소. 다음 예정일 재계산 | HLT-003-12 | JAJ | - | - |
| DELETE | /pets/{petSn}/care-items/{careArtclSn} | 사용자 추가 케어 항목 삭제(기본 항목 불가) | HLT-003-12 | JAJ | - | - |
| DELETE | /pets/{petSn}/care-schedules/{schdlSn} | 일정 삭제(연결 검진기록은 유지) | HLT-003-33 | JAJ | - | - |
| DELETE | /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn} | 첨부 삭제 | HLT-005-01 | JAJ | - | - |
| DELETE | /pets/{petSn}/checkups/{ckupRcdSn} | 검진기록 삭제(자동 생성 케어 항목 삭제, 완료 기록·병원 일정 유지) | HLT-002-05 | JAJ | - | - |
| DELETE | /pets/{petSn}/daily-health-records/{dailyHlthRcdSn} | 일상 기록 삭제 | HLT-002-13 | JAJ | - | - |
| DELETE | /pets/{petSn}/etc-inspections/{etcInspSn} | 기타검사 삭제 | HLT-005-02 | JAJ | - | - |
| DELETE | /pets/{petSn}/inspection-values | 검사 수치 선택 삭제. `?inspNvlSn=1&inspNvlSn=2` | HLT-005-02 | JAJ | - | - |
| DELETE | /pets/{petSn}/observation-cycles/{obsrvCyclSn} | 관찰 회차 삭제(마지막 회차면 항목도 삭제) | HLT-002-19 | JAJ | - | - |
| DELETE | /pets/{petSn}/prescriptions/{prscSn} | 처방약 삭제(자동 투약 항목 삭제) | HLT-005-03 | JAJ | - | - |
| DELETE | /pets/{petSn}/repeat-preventions/{reptPrvntMngSn} | 반복 예방 해지(자동 케어 항목 삭제, 완료 기록 유지) | HLT-005-05 | JAJ | - | - |

## DTO

### CareCompletionCreateRequest
쓰는 API: POST /pets/{petSn}/care-completions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclSn | Long | Y | 이 반려동물의 케어 항목. 투약 항목은 1회 체크마다 1건(n/N회). 완료일은 요청에 없음(issues 4) | CARE_CMPTN_HSTRY.CARE_ARTCL_SN, HLT-003-12 |

### CareCompletionResponse
쓰는 API: POST /pets/{petSn}/care-completions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careCmptnHstrySn | Long | Y | | CARE_CMPTN_HSTRY.CARE_CMPTN_HSTRY_SN |
| careArtclSn | Long | Y | | CARE_CMPTN_HSTRY.CARE_ARTCL_SN |
| cmptnYmd | LocalDate | Y | | CARE_CMPTN_HSTRY.CMPTN_YMD |
| cmptnCycl | Integer | N | 투약 항목의 몇 번째 체크인지 | CARE_CMPTN_HSTRY.CMPTN_CYCL, HLT-003-12 |
| nxtmPrnmntYmd | LocalDate | N | 재계산한 다음 예정일(정기관리) | CARE_ARTCL.NXTM_PRNMNT_YMD, HLT-003-12 |
| lastCmptnYmd | LocalDate | N | | CARE_ARTCL.LAST_CMPTN_YMD |
| todayCmptnCnt | Integer | N | 오늘 완료 건수(투약 n/N회의 n) | HLT-003-12 |

### CareCompletionSummary
쓰는 API: GET /pets/{petSn}/care-completions ("기록 내용" 목록)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careCmptnHstrySn | Long | Y | 취소(DELETE) 대상 | CARE_CMPTN_HSTRY.CARE_CMPTN_HSTRY_SN |
| careArtclSn | Long | Y | | CARE_CMPTN_HSTRY.CARE_ARTCL_SN |
| careArtclNm | String | Y | | CARE_ARTCL.CARE_ARTCL_NM, HLT-003-12 |
| rprsIconId | String | Y | 기록 내용의 항목 아이콘(클릭 시 취소) | CARE_ARTCL.RPRS_ICON_ID, HLT-003-12 |
| cmptnYmd | LocalDate | Y | 날짜별로 쌓아 표시 | CARE_CMPTN_HSTRY.CMPTN_YMD, HLT-003-12 |
| cmptnCycl | Integer | N | | CARE_CMPTN_HSTRY.CMPTN_CYCL |

### CareItemCreateRequest
쓰는 API: POST /pets/{petSn}/care-items
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclNm | String | Y | 최대 40자. 같은 이름 허용(HLT-003-21, AIM-012-08) | CARE_ARTCL.CARE_ARTCL_NM (HLT-003-21) |
| rprsIconId | String | N | 최대 20자. 미입력 시 기본 아이콘 | CARE_ARTCL.RPRS_ICON_ID (미입력 시 기본 아이콘, HLT-003-22·AIM-012-13) |
| careZoneCd | String | Y | HLTG13 공통코드. 매일 = 일상관리, 주기 = 정기관리 | CARE_ARTCL.CARE_ZONE_CD (반복 유형 매일/주기, HLT-003-23) |
| careCycleCd | String | N | HLTG15 공통코드(1주·2주·한달·1년·직접입력). 정기관리면 필수 | CARE_ARTCL.CARE_CYCLE_CD (주기 선택 시 필수, HLT-003-24) |
| cycleDrctInptDayCnt | Integer | N | 직접입력이면 필수. 최대 4자리 | CARE_ARTCL.CYCLE_DRCT_INPT_DAY_CNT (직접입력 시 필수) |
| lastCmptnYmd | LocalDate | N | 정기관리만. 오늘 이후 날짜 불가(HLT-003-38). 미입력 시 등록일 기준으로 예정일 계산 | CARE_CMPTN_HSTRY.CMPTN_YMD (HLT-003-38, 미입력 시 등록일 기준) |

### CareItemExposureSaveRequest
쓰는 API: PUT /pets/{petSn}/care-item-exposures
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclSns | List<Long> | Y | 최대 6개(HLT-003-13). 목록 순서 = 메인 정렬 순서, 목록에 없는 항목은 메인 표시 해제. 빈 목록 허용 여부는 명세에 없음 | CARE_ARTCL.CARE_ARTCL_SN — 목록 순서 = MAIN_SORT_SEQ, 목록에 없는 항목은 MAIN_EXPSR_YN='N', 최대 6개(HLT-003-13) |

### CareItemResponse
쓰는 API: PATCH /pets/{petSn}/care-items/{careArtclSn}; POST /pets/{petSn}/care-items (GET care-items 응답 행과 같은 필드 → CareItemSummary와 같은 모양)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclSn | Long | Y | | CARE_ARTCL.CARE_ARTCL_SN |
| careArtclNm | String | Y | | CARE_ARTCL.CARE_ARTCL_NM |
| careZoneCd | String | Y | HLTG13 | CARE_ARTCL.CARE_ZONE_CD |
| careCycleCd | String | N | HLTG15 | CARE_ARTCL.CARE_CYCLE_CD |
| careCycleCdNm | String | N | 타일의 주기 표시 | COM_CD.COM_CD_NM, HLT-003-12 (추가) |
| cycleDrctInptDayCnt | Integer | N | | CARE_ARTCL.CYCLE_DRCT_INPT_DAY_CNT |
| rprsIconId | String | Y | | CARE_ARTCL.RPRS_ICON_ID |
| nxtmPrnmntYmd | LocalDate | N | | CARE_ARTCL.NXTM_PRNMNT_YMD |
| lastCmptnYmd | LocalDate | N | | CARE_ARTCL.LAST_CMPTN_YMD |
| mainExpsrYn | boolean | Y | | CARE_ARTCL.MAIN_EXPSR_YN |
| mainSortSeq | Integer | N | | CARE_ARTCL.MAIN_SORT_SEQ |
| bscArtclYn | boolean | Y | | CARE_ARTCL.BSC_ARTCL_YN |
| prscSn | Long | N | | CARE_ARTCL.PRSC_SN (처방약 연동 뱃지·출처 보기) |
| reptPrvntMngSn | Long | N | | CARE_ARTCL.REPT_PRVNT_MNG_SN (출처 보기) |
| ckupRcdSn | Long | N | 자동 생성 항목의 출처 검진기록([출처 보기] → 검진상세) | PRSC.CKUP_RCD_SN 또는 REPT_PRVNT_MNG.CKUP_RCD_SN, HLT-003-12 (추가) |
| mdcnEndYmd | LocalDate | N | | PRSC.MDCN_END_YMD (HLT-003-12 복용 종료일 표시) |
| dayMdcnNmtm | Integer | N | | PRSC.DAY_MDCN_NMTM (투약 n/N회의 N) |
| todayCmptnCnt | Integer | N | | HLT-003-12·13 (오늘 CARE_CMPTN_HSTRY 건수) |

### CareItemSummary
쓰는 API: GET /pets/{petSn}/care-items; PUT /pets/{petSn}/care-item-exposures (응답 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclSn | Long | Y | | CARE_ARTCL.CARE_ARTCL_SN |
| careArtclNm | String | Y | | CARE_ARTCL.CARE_ARTCL_NM |
| careZoneCd | String | Y | HLTG13 | CARE_ARTCL.CARE_ZONE_CD |
| careCycleCd | String | N | HLTG15 | CARE_ARTCL.CARE_CYCLE_CD |
| careCycleCdNm | String | N | 타일의 주기 표시 | COM_CD.COM_CD_NM, HLT-003-12 (추가) |
| cycleDrctInptDayCnt | Integer | N | | CARE_ARTCL.CYCLE_DRCT_INPT_DAY_CNT |
| rprsIconId | String | Y | | CARE_ARTCL.RPRS_ICON_ID |
| nxtmPrnmntYmd | LocalDate | N | 상태 배지(오늘/D-n/+n일)는 화면이 계산 | CARE_ARTCL.NXTM_PRNMNT_YMD |
| lastCmptnYmd | LocalDate | N | | CARE_ARTCL.LAST_CMPTN_YMD |
| mainExpsrYn | boolean | Y | | CARE_ARTCL.MAIN_EXPSR_YN |
| mainSortSeq | Integer | N | | CARE_ARTCL.MAIN_SORT_SEQ |
| bscArtclYn | boolean | Y | | CARE_ARTCL.BSC_ARTCL_YN |
| prscSn | Long | N | | CARE_ARTCL.PRSC_SN (처방약 연동 뱃지·출처 보기) |
| reptPrvntMngSn | Long | N | | CARE_ARTCL.REPT_PRVNT_MNG_SN (출처 보기) |
| ckupRcdSn | Long | N | 자동 생성 항목의 출처 검진기록([출처 보기] → 검진상세) | PRSC.CKUP_RCD_SN 또는 REPT_PRVNT_MNG.CKUP_RCD_SN, HLT-003-12 (추가) |
| mdcnEndYmd | LocalDate | N | | PRSC.MDCN_END_YMD (HLT-003-12 복용 종료일 표시) |
| dayMdcnNmtm | Integer | N | | PRSC.DAY_MDCN_NMTM (투약 n/N회의 N) |
| todayCmptnCnt | Integer | N | | HLT-003-12·13 (오늘 CARE_CMPTN_HSTRY 건수) |

### CareItemUpdateRequest
쓰는 API: PATCH /pets/{petSn}/care-items/{careArtclSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careArtclNm | String | N | 최대 40자. 기본 항목은 변경 불가 | CARE_ARTCL.CARE_ARTCL_NM, HLT-003-12 |
| rprsIconId | String | N | 최대 20자. 기본 항목은 변경 불가 | CARE_ARTCL.RPRS_ICON_ID, HLT-003-12 |
| careZoneCd | String | N | HLTG13. 기본 항목은 변경 불가 | CARE_ARTCL.CARE_ZONE_CD, HLT-003-12 |
| careCycleCd | String | N | HLTG15. 기본 정기관리 항목은 이 값과 직접입력 일수만 변경 가능 | CARE_ARTCL.CARE_CYCLE_CD, HLT-003-12 |
| cycleDrctInptDayCnt | Integer | N | 직접입력이면 필수. 최대 4자리 | CARE_ARTCL.CYCLE_DRCT_INPT_DAY_CNT, HLT-003-24 |

### CareScheduleCreateRequest
쓰는 API: POST /pets/{petSn}/care-schedules (AI 대화 등록도 같은 API)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| schdlKndCd | String | Y | HLTG19 공통코드(병원·미용·목욕·케어·투약·기타) | SCHDL.SCHDL_KND_CD (HLT-003-34) |
| schdlDt | LocalDateTime | Y | 날짜 필수. 시간 미입력이면 `hrInptYn=false`이고 시각은 무시 | SCHDL.SCHDL_DT (HLT-003-32) |
| hrInptYn | Boolean | Y | | SCHDL.HR_INPT_YN (시간 선택 입력, HLT-003-32) |
| ckupTypeCd | String | N | HLTG01 공통코드(5종). 병원 일정이면 필수(서버 재검증) | SCHDL.CKUP_TYPE_CD (병원 일정이면 필수, AIM-012-12) |
| hsptlNm | String | N | 최대 80자. 병원 일정이면 필수(서버 재검증) | SCHDL.HSPTL_NM (병원 일정이면 필수, AIM-012-12) |
| schdlNm | String | N | 최대 40자. 병원 외 종류면 필수(서버 재검증) | SCHDL.SCHDL_NM (병원 외 종류면 필수, AIM-012-12) |
| memoCn | String | N | 최대 500자. 일정 메모(검진 메모와 별개) | SCHDL.MEMO_CN (HLT-003-36) |
| cldrBadgeColrCd | String | N | HLTG21 공통코드 | SCHDL.CLDR_BADGE_COLR_CD (미지정 시 종류 기본 색상, HLT-003-36) |
| schdlSrcCd | String | N | HLTG22 공통코드 | SCHDL.SCHDL_SRC_CD (AI 대화 등록은 'AI 생성', AIM-012-13) |

### CareScheduleResponse
쓰는 API: PATCH /pets/{petSn}/care-schedules/{schdlSn}; POST /pets/{petSn}/care-schedules
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| schdlSn | Long | Y | | SCHDL.SCHDL_SN |
| petSn | Long | Y | 수정으로 반려동물이 바뀌면 바뀐 값 | SCHDL.PET_SN |
| schdlKndCd | String | Y | | SCHDL.SCHDL_KND_CD (HLT-003-34) |
| schdlDt | LocalDateTime | Y | | SCHDL.SCHDL_DT (HLT-003-32) |
| hrInptYn | boolean | Y | | SCHDL.HR_INPT_YN (HLT-003-32) |
| ckupTypeCd | String | N | | SCHDL.CKUP_TYPE_CD |
| hsptlNm | String | N | | SCHDL.HSPTL_NM |
| schdlNm | String | N | | SCHDL.SCHDL_NM |
| memoCn | String | N | | SCHDL.MEMO_CN (HLT-003-36) |
| cldrBadgeColrCd | String | N | 미지정이면 null(기본 색상 적용 방식은 명세에 없음) | SCHDL.CLDR_BADGE_COLR_CD (HLT-003-36) |
| schdlSrcCd | String | N | | SCHDL.SCHDL_SRC_CD (AIM-012-13) |

### CareScheduleSummary
쓰는 API: GET /pets/{petSn}/care-schedules (월간·날짜 상세·다가오는 일정·다음 예정일·가까운 예정 일정 후보)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| schdlSn | Long | Y | | SCHDL.SCHDL_SN |
| petSn | Long | Y | 통합 캘린더에서 반려동물 이름 표기용 | SCHDL.PET_SN, HLT-003-30·31 |
| schdlKndCd | String | Y | HLTG19 | SCHDL.SCHDL_KND_CD |
| schdlKndCdNm | String | Y | 일정 종류 태그 | COM_CD.COM_CD_NM, HLT-003-30 |
| schdlDt | LocalDateTime | Y | | SCHDL.SCHDL_DT, HLT-003-29·40 |
| hrInptYn | boolean | Y | 시간 표시·정렬(시간 입력 건 먼저) | SCHDL.HR_INPT_YN, HLT-003-32·40 |
| ckupTypeCd | String | N | HLTG01. 병원 일정만 | SCHDL.CKUP_TYPE_CD, HLT-011-04 |
| ckupTypeCdNm | String | N | 병원 일정의 검진유형 병기 | COM_CD.COM_CD_NM, HLT-003-30·40 |
| hsptlNm | String | N | | SCHDL.HSPTL_NM, HLT-003-40 |
| schdlNm | String | N | | SCHDL.SCHDL_NM, HLT-003-40 |
| memoCn | String | N | 날짜 인라인 상세에만 표시 | SCHDL.MEMO_CN, HLT-003-36 |
| cldrBadgeColrCd | String | N | HLTG21. 일정 행 색상 칩 | SCHDL.CLDR_BADGE_COLR_CD, HLT-003-31 |
| ckupRcdCnt | Integer | Y | 연결된 검진기록 수. 0이면 [검진기록 없음], 가까운 예정 일정 후보는 0건인 병원 일정 | HLT-002-05·46, HLT-003-31 (CKUP_RCD.SCHDL_SN 건수) |

### CareScheduleUpdateRequest
쓰는 API: PATCH /pets/{petSn}/care-schedules/{schdlSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | N | 옮길 반려동물. 연결 검진기록·자동 생성 케어 항목·완료 기록·반복 예방 관리도 함께 이동, 옮긴 항목의 ★ 해제(`issues.md` API의 AI매니저·건강기록 9번) | SCHDL.PET_SN, HLT-003-37 |
| schdlKndCd | String | N | HLTG19. 종류를 바꾸면 이전 종류의 입력값은 유지하지 않음 | SCHDL.SCHDL_KND_CD, HLT-003-34·35 |
| schdlDt | LocalDateTime | N | | SCHDL.SCHDL_DT, HLT-003-32 |
| hrInptYn | Boolean | N | | SCHDL.HR_INPT_YN, HLT-003-32 |
| ckupTypeCd | String | N | HLTG01. 수정 결과가 병원 일정이면 필수 | SCHDL.CKUP_TYPE_CD, HLT-003-35 |
| hsptlNm | String | N | 최대 80자. 수정 결과가 병원 일정이면 필수 | SCHDL.HSPTL_NM, HLT-003-35 |
| schdlNm | String | N | 최대 40자. 수정 결과가 병원 외 종류면 필수 | SCHDL.SCHDL_NM, HLT-003-35 |
| memoCn | String | N | 최대 500자 | SCHDL.MEMO_CN, HLT-003-36 |
| cldrBadgeColrCd | String | N | HLTG21 | SCHDL.CLDR_BADGE_COLR_CD, HLT-003-36 |

### CheckupAttachmentCreateRequest
쓰는 API: POST /pets/{petSn}/checkup-attachments (multipart/form-data)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 이 반려동물의 검진기록 | CKUP_RCD_ATCH.CKUP_RCD_SN, HLT-005-01 |
| file | MultipartFile | Y | 형식·용량 제약은 명세에 없음(issues 6) | CKUP_RCD_ATCH.ATCH_FILE_SN, HLT-005-01 |
| atchTypeCd | String | Y | HLTG20 공통코드(검사결과지·접종 증명서·영상 사진) | CKUP_RCD_ATCH.ATCH_TYPE_CD, HLT-005-01 |
| imgKndCd | String | N | HLTG23 공통코드(엑스레이·초음파·CT·MRI·기타). 영상 사진이면 필수 | CKUP_RCD_ATCH.IMG_KND_CD, HLT-005-01 |
| shtRegnCd | String | N | HLTG24 공통코드(흉부·복부·관절·치아·기타). 영상 사진이면 필수 | CKUP_RCD_ATCH.SHT_REGN_CD, HLT-005-01 |

### CheckupAttachmentResponse
쓰는 API: PATCH /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn}; POST /pets/{petSn}/checkup-attachments (CheckupAttachmentSummary와 같은 모양)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdAtchSn | Long | Y | 파일은 GET …/checkup-attachments/{ckupRcdAtchSn} | CKUP_RCD_ATCH.CKUP_RCD_ATCH_SN |
| ckupRcdSn | Long | Y | | CKUP_RCD_ATCH.CKUP_RCD_SN |
| atchTypeCd | String | Y | HLTG20 | CKUP_RCD_ATCH.ATCH_TYPE_CD |
| atchTypeCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-005-01 |
| imgKndCd | String | N | HLTG23 | CKUP_RCD_ATCH.IMG_KND_CD |
| imgKndCdNm | String | N | | COM_CD.COM_CD_NM, HLT-005-02 |
| shtRegnCd | String | N | HLTG24 | CKUP_RCD_ATCH.SHT_REGN_CD |
| shtRegnCdNm | String | N | | COM_CD.COM_CD_NM, HLT-005-02 |
| orgnlFileNm | String | Y | | ATCH_FILE.ORGNL_FILE_NM, HLT-011-09 |
| fileExtnCd | String | Y | FILG01. PDF·이미지 구분(다운로드/미리보기 분기) | ATCH_FILE.FILE_EXTN_CD, HLT-011-09 |
| regDt | LocalDateTime | Y | 첨부 카드의 날짜(issues 7) | CKUP_RCD_ATCH.REG_DT, HLT-011-09 |

### CheckupAttachmentSummary
쓰는 API: GET /pets/{petSn}/checkup-attachments (최신순, 영상검사 칩도 이 목록)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdAtchSn | Long | Y | 파일은 GET …/checkup-attachments/{ckupRcdAtchSn} | CKUP_RCD_ATCH.CKUP_RCD_ATCH_SN |
| ckupRcdSn | Long | Y | [전체 보기] → 그 검진상세 | CKUP_RCD_ATCH.CKUP_RCD_SN, HLT-011-09 |
| atchTypeCd | String | Y | HLTG20. 검사결과지 PDF만 [PDF 분석] | CKUP_RCD_ATCH.ATCH_TYPE_CD, HLT-005-04 |
| atchTypeCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-005-01 |
| imgKndCd | String | N | HLTG23 | CKUP_RCD_ATCH.IMG_KND_CD |
| imgKndCdNm | String | N | 영상검사 칩의 영상 종류 | COM_CD.COM_CD_NM, HLT-005-02 |
| shtRegnCd | String | N | HLTG24 | CKUP_RCD_ATCH.SHT_REGN_CD |
| shtRegnCdNm | String | N | 영상검사 칩의 촬영 부위 | COM_CD.COM_CD_NM, HLT-005-02 |
| orgnlFileNm | String | Y | | ATCH_FILE.ORGNL_FILE_NM, HLT-011-09 |
| fileExtnCd | String | Y | FILG01. PDF·이미지 구분 | ATCH_FILE.FILE_EXTN_CD, HLT-011-09 |
| regDt | LocalDateTime | Y | 첨부 카드의 날짜(issues 7) | CKUP_RCD_ATCH.REG_DT, HLT-011-09 |

### CheckupAttachmentUpdateRequest
쓰는 API: PATCH /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn} (multipart/form-data)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| file | MultipartFile | N | 이 첨부 1건의 파일만 교체. 형식·용량 제약은 명세에 없음(issues 6) | CKUP_RCD_ATCH.ATCH_FILE_SN, HLT-005-01 |
| atchTypeCd | String | N | HLTG20 | CKUP_RCD_ATCH.ATCH_TYPE_CD, HLT-005-01 |
| imgKndCd | String | N | HLTG23. 수정 결과가 영상 사진이면 필수 | CKUP_RCD_ATCH.IMG_KND_CD, HLT-005-01 |
| shtRegnCd | String | N | HLTG24. 수정 결과가 영상 사진이면 필수 | CKUP_RCD_ATCH.SHT_REGN_CD, HLT-005-01 |

### CheckupCreateRequest
쓰는 API: POST /pets/{petSn}/checkups (직접 입력)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupYmd | LocalDate | Y | 기본값 오늘, 미래 날짜 허용. 같은 날짜 병원 일정에 자동 연결(2건 이상이면 먼저 등록된 일정), 없으면 새로 만듦 | HLT-002-27·30, SCHDL.SCHDL_DT (CKUP_RCD에 검진일 컬럼 없음, `issues.md` API의 AI매니저·건강기록 1번) |
| hsptlNm | String | Y | 최대 80자(직접 입력은 명세에 길이 없음, issues 11) | CKUP_RCD.HSPTL_NM, HLT-002-28 |
| ckupTypeCd | String | Y | HLTG01 공통코드(정기검진·예방접종·질병진료·검사·기타 진료) | CKUP_RCD.CKUP_TYPE_CD, HLT-002-28 |
| ckupCn | String | N | 최대 500자 | CKUP_RCD.CKUP_CN, HLT-002-29 |
| ckupMemoCn | String | N | 최대 200자. 일정 메모와 별개 | CKUP_RCD.CKUP_MEMO_CN, HLT-002-29 |
| schdlSn | Long | N | [옮겨 연결하기]를 고른 경우만. 같은 반려동물·검진기록 미연결 병원 일정, 검진일 앞뒤 7일. 일정 날짜를 검진일로 옮기고 연결 | CKUP_RCD.SCHDL_SN, HLT-002-46 |

### CheckupResponse
쓰는 API: GET /pets/{petSn}/checkups/{ckupRcdSn}; PATCH /pets/{petSn}/checkups/{ckupRcdSn}; POST /pets/{petSn}/checkups; POST /receipts/{recptSn}/checkups (expense? API도 이 DTO를 씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | | CKUP_RCD.CKUP_RCD_SN |
| petSn | Long | Y | | AIM-009-12·25 (반려동물별 검진상세 이동) |
| schdlSn | Long | N | 연결 병원 일정 | CKUP_RCD.SCHDL_SN, HLT-002-05 (추가) |
| ckupYmd | LocalDate | N | 검진일 | SCHDL.SCHDL_DT, HLT-005-01 (CKUP_RCD에 검진일 컬럼 없음, `issues.md` API의 AI매니저·건강기록 1번) (추가) |
| hsptlNm | String | Y | | CKUP_RCD.HSPTL_NM, HLT-005-01 (추가) |
| ckupTypeCd | String | Y | HLTG01 | CKUP_RCD.CKUP_TYPE_CD, HLT-005-01 (추가) |
| ckupTypeCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-005-01 (추가) |
| ckupCn | String | N | | CKUP_RCD.CKUP_CN, HLT-005-01 (추가) |
| ckupMemoCn | String | N | | CKUP_RCD.CKUP_MEMO_CN, HLT-005-01 (추가) |
| recptSn | Long | N | 영수증으로 등록한 기록이면 연결된 영수증 | CKUP_RCD.RECPT_SN, HLT-002-31·45 (추가) |

### CheckupSummary
쓰는 API: GET /pets/{petSn}/checkups (리스트·필터 칩 건수·타임라인·예방/정기검진 이력)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 병원명 클릭 → 검진상세 | CKUP_RCD.CKUP_RCD_SN, HLT-002-05, HLT-011-03 |
| schdlSn | Long | N | | CKUP_RCD.SCHDL_SN |
| ckupYmd | LocalDate | N | 검진일(정렬·연도 필터 기준) | SCHDL.SCHDL_DT, HLT-002-14, HLT-011-03·04 (`issues.md` API의 AI매니저·건강기록 1번) |
| hsptlNm | String | Y | | CKUP_RCD.HSPTL_NM, HLT-002-05 |
| ckupTypeCd | String | Y | HLTG01. 예방접종·정기검진 이력 구분 | CKUP_RCD.CKUP_TYPE_CD, HLT-002-06, HLT-011-04 |
| ckupTypeCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-011-03 |
| schdlDelYn | boolean | Y | 연결 일정이 삭제됐으면 true → "연결된 일정 삭제됨" | SCHDL.DEL_YN, HLT-002-05 (issues 12) |

### CheckupUpdateRequest
쓰는 API: PATCH /pets/{petSn}/checkups/{ckupRcdSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | N | 옮길 반려동물. 그 반려동물의 같은 날짜 병원 일정으로 재연결(없으면 생성), 자동 생성 케어 항목·완료 기록·반복 예방 관리도 이동, ★ 해제(`issues.md` API의 AI매니저·건강기록 9번) | SCHDL.PET_SN, HLT-002-26 |
| ckupYmd | LocalDate | N | 바꾸면 같은 반려동물·같은 날짜 병원 일정으로 재연결(없으면 생성). 앞뒤 7일 확인 없음 | HLT-005-01, SCHDL.SCHDL_DT (`issues.md` API의 AI매니저·건강기록 1번) |
| hsptlNm | String | N | 최대 80자. 일정의 병원명은 바꾸지 않음 | CKUP_RCD.HSPTL_NM, HLT-005-01 |
| ckupTypeCd | String | N | HLTG01. 일정의 검진유형은 바꾸지 않음 | CKUP_RCD.CKUP_TYPE_CD, HLT-005-01 |
| ckupCn | String | N | 최대 500자 | CKUP_RCD.CKUP_CN, HLT-005-01 |
| ckupMemoCn | String | N | 최대 200자 | CKUP_RCD.CKUP_MEMO_CN, HLT-005-01 |

### DailyHealthRecordCreateRequest
쓰는 API: POST /pets/{petSn}/daily-health-records
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dailyRcdIdctCd | String | Y | HLTG09 공통코드(체중·식사량·음수량·배변상태·산책) | DAILY_HLTH_RCD.DAILY_RCD_IDCT_CD, HLT-002-11 |
| rcdDt | LocalDateTime | Y | 화면은 시간만 입력, 날짜는 오늘("오늘 기록하기") | DAILY_HLTH_RCD.RCD_DT, HLT-002-11 |
| rcdNvl | BigDecimal | N | 식사량 g·음수량 ml·산책 분·체중 kg. 배변상태가 아니면 필수. 정수 5자리·소수 3자리 | DAILY_HLTH_RCD.RCD_NVL, HLT-002-11 |
| crsdSttsCd | String | N | HLTG10 공통코드(정상·딱딱함·무름·설사·변비). 배변상태면 필수 | DAILY_HLTH_RCD.CRSD_STTS_CD, HLT-002-11 |

### DailyHealthRecordResponse
쓰는 API: PATCH /pets/{petSn}/daily-health-records/{dailyHlthRcdSn}; POST /pets/{petSn}/daily-health-records (DailyHealthRecordSummary와 같은 모양)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dailyHlthRcdSn | Long | Y | | DAILY_HLTH_RCD.DAILY_HLTH_RCD_SN |
| dailyRcdIdctCd | String | Y | HLTG09 | DAILY_HLTH_RCD.DAILY_RCD_IDCT_CD |
| rcdNvl | BigDecimal | N | | DAILY_HLTH_RCD.RCD_NVL |
| crsdSttsCd | String | N | HLTG10 | DAILY_HLTH_RCD.CRSD_STTS_CD |
| crsdSttsCdNm | String | N | | COM_CD.COM_CD_NM, HLT-002-12·13 |
| rcdDt | LocalDateTime | Y | | DAILY_HLTH_RCD.RCD_DT |

### DailyHealthRecordSummary
쓰는 API: GET /pets/{petSn}/daily-health-records (그래프·오늘의 기록·날짜별 표·최신 체중)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dailyHlthRcdSn | Long | Y | | DAILY_HLTH_RCD.DAILY_HLTH_RCD_SN |
| dailyRcdIdctCd | String | Y | HLTG09 | DAILY_HLTH_RCD.DAILY_RCD_IDCT_CD |
| rcdNvl | BigDecimal | N | | DAILY_HLTH_RCD.RCD_NVL (양·체중·산책 분) |
| crsdSttsCd | String | N | HLTG10 | DAILY_HLTH_RCD.CRSD_STTS_CD (배변상태) |
| crsdSttsCdNm | String | N | 표·타일의 배변상태 이름 | COM_CD.COM_CD_NM, HLT-002-12·13 (추가) |
| rcdDt | LocalDateTime | Y | | DAILY_HLTH_RCD.RCD_DT |

### DailyHealthRecordUpdateRequest
쓰는 API: PATCH /pets/{petSn}/daily-health-records/{dailyHlthRcdSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rcdDt | LocalDateTime | N | 인라인 편집의 시각 | DAILY_HLTH_RCD.RCD_DT, HLT-002-13 |
| rcdNvl | BigDecimal | N | 정수 5자리·소수 3자리 | DAILY_HLTH_RCD.RCD_NVL, HLT-002-13 |
| crsdSttsCd | String | N | HLTG10. 배변상태 기록만 | DAILY_HLTH_RCD.CRSD_STTS_CD, HLT-002-13 |

### EtcInspectionCreateRequest
쓰는 API: POST /pets/{petSn}/etc-inspections
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 이 반려동물의 검진기록 | ETC_INSP.CKUP_RCD_SN, HLT-005-02 |
| inspNm | String | Y | 최대 100자 | ETC_INSP.INSP_NM, HLT-005-11 |
| inspRsltCn | String | Y | 1~500자, 텍스트만 | ETC_INSP.INSP_RSLT_CN, HLT-005-12 |
| memoCn | String | N | 최대 200자 | ETC_INSP.MEMO_CN, HLT-005-13 |

### EtcInspectionResponse
쓰는 API: PATCH /pets/{petSn}/etc-inspections/{etcInspSn}; POST /pets/{petSn}/etc-inspections
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| etcInspSn | Long | Y | | ETC_INSP.ETC_INSP_SN |
| ckupRcdSn | Long | Y | | ETC_INSP.CKUP_RCD_SN |
| inspNm | String | Y | | ETC_INSP.INSP_NM |
| inspRsltCn | String | Y | | ETC_INSP.INSP_RSLT_CN |
| memoCn | String | N | | ETC_INSP.MEMO_CN |

### EtcInspectionSummary
쓰는 API: GET /pets/{petSn}/etc-inspections
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| etcInspSn | Long | Y | | ETC_INSP.ETC_INSP_SN |
| ckupRcdSn | Long | Y | | ETC_INSP.CKUP_RCD_SN |
| inspNm | String | Y | | ETC_INSP.INSP_NM, HLT-005-02 |
| inspRsltCn | String | Y | | ETC_INSP.INSP_RSLT_CN, HLT-005-02 |
| memoCn | String | N | 없으면 화면에서 생략 | ETC_INSP.MEMO_CN, HLT-005-02 |

### EtcInspectionUpdateRequest
쓰는 API: PATCH /pets/{petSn}/etc-inspections/{etcInspSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspNm | String | N | 최대 100자. 비울 수 없음(필수 항목) | ETC_INSP.INSP_NM, HLT-005-11 |
| inspRsltCn | String | N | 1~500자. 비울 수 없음(필수 항목) | ETC_INSP.INSP_RSLT_CN, HLT-005-12 |
| memoCn | String | N | 최대 200자 | ETC_INSP.MEMO_CN, HLT-005-13 |

### InspectionAnalysisItem
쓰는 API: POST /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn}/analyses (InspectionAnalysisResponse.items의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspArtclCd | String | N | HLTG12. 12종 중 인식한 항목 | INSP_NVL.INSP_ARTCL_CD, HLT-007-04 |
| inspArtclDrctInptNm | String | N | 12종에 없는 항목명 | INSP_NVL.INSP_ARTCL_DRCT_INPT_NM, HLT-007-04 |
| rsltNvl | BigDecimal | N | 인식 실패·저품질이면 null | INSP_NVL.RSLT_NVL, HLT-007-04 |
| unitNm | String | N | 인식 실패면 null | INSP_NVL.UNIT_NM, HLT-007-04 |
| refLlmtNvl | BigDecimal | N | | INSP_NVL.REF_LLMT_NVL, HLT-007-04 |
| refUplmtNvl | BigDecimal | N | | INSP_NVL.REF_UPLMT_NVL, HLT-007-04 |
| registered | Boolean | Y | 같은 검진기록에 이미 있는 항목이면 true → "이미 등록됨", 저장 대상 제외 | HLT-007-04 |

### InspectionAnalysisResponse
쓰는 API: POST /pets/{petSn}/checkup-attachments/{ckupRcdAtchSn}/analyses (저장 안 함)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdAtchSn | Long | Y | 분석한 첨부 | CKUP_RCD_ATCH.CKUP_RCD_ATCH_SN, HLT-005-04 |
| ckupRcdSn | Long | Y | 저장할 검진기록(SCR-HLT-007 진입) | CKUP_RCD_ATCH.CKUP_RCD_SN, HLT-005-04 |
| anlysRslt | String | Y | 분석 결과 구분: 성공 / 인식 실패 / 일시 오류(제한 시간 초과·외부 API 오류). 실패도 200으로 주고 이 값으로 문구를 고른다. 값 표현은 영수증의 `ocrPrcsSttsCd`(AIMG01)처럼 공통코드로 둘지 JAJ가 정한다(2026-10-22 제안) | HLT-005-04, HLT-007-06 |
| items | List<InspectionAnalysisItem> | Y | 혈액검사 항목만. 0건이면 빈 목록 | HLT-007-04·05 |
| excludedArtclNms | List<String> | N | 혈액검사가 아니라서 뺀 항목 이름(안내용, issues 10) | HLT-007-05 |

### InspectionValueCreateRequest
쓰는 API: POST /pets/{petSn}/inspection-values (혈액검사 고정)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 이 반려동물의 검진기록 | INSP_NVL.CKUP_RCD_SN, HLT-007-03 |
| values | List<InspectionValueItemRequest> | Y | 1행 이상. 같은 검진기록에 이미 있는 항목은 저장에서 제외, 삭제된 같은 항목은 복구(issues 1) | HLT-007-02·03, HLT-005-02 |

### InspectionValueItemRequest
쓰는 API: POST /pets/{petSn}/inspection-values (InspectionValueCreateRequest.values의 요소. 중첩 record)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspArtclCd | String | N | HLTG12 공통코드(12종). 이 값과 직접입력명 중 하나는 필수 | INSP_NVL.INSP_ARTCL_CD, HLT-007-02 |
| inspArtclDrctInptNm | String | N | 최대 40자. 12종에 없을 때 직접입력 | INSP_NVL.INSP_ARTCL_DRCT_INPT_NM, HLT-007-02 |
| rsltNvl | BigDecimal | Y | 정수 11자리·소수 4자리 | INSP_NVL.RSLT_NVL, HLT-007-02 |
| unitNm | String | Y | 최대 20자 | INSP_NVL.UNIT_NM, HLT-007-02 |
| refLlmtNvl | BigDecimal | N | 상한과 함께 입력하거나 둘 다 비움 | INSP_NVL.REF_LLMT_NVL, HLT-007-02 |
| refUplmtNvl | BigDecimal | N | 하한과 함께 입력하거나 둘 다 비움 | INSP_NVL.REF_UPLMT_NVL, HLT-007-02 |
| memoCn | String | N | 최대 200자 | INSP_NVL.MEMO_CN, HLT-007-02 |

### InspectionValueResponse
쓰는 API: PATCH /pets/{petSn}/inspection-values/{inspNvlSn}; POST /pets/{petSn}/inspection-values (POST는 List)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspNvlSn | Long | Y | | INSP_NVL.INSP_NVL_SN |
| ckupRcdSn | Long | Y | | INSP_NVL.CKUP_RCD_SN |
| inspArtclCd | String | N | HLTG12 | INSP_NVL.INSP_ARTCL_CD |
| inspArtclDrctInptNm | String | N | | INSP_NVL.INSP_ARTCL_DRCT_INPT_NM |
| rsltNvl | BigDecimal | Y | | INSP_NVL.RSLT_NVL |
| unitNm | String | Y | | INSP_NVL.UNIT_NM |
| refLlmtNvl | BigDecimal | N | | INSP_NVL.REF_LLMT_NVL |
| refUplmtNvl | BigDecimal | N | | INSP_NVL.REF_UPLMT_NVL |
| memoCn | String | N | | INSP_NVL.MEMO_CN |
| sortSeq | Integer | N | | INSP_NVL.SORT_SEQ |

### InspectionValueSummary
쓰는 API: GET /pets/{petSn}/inspection-values (검진상세 결과·최근 검사·수치 추이)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspNvlSn | Long | Y | | INSP_NVL.INSP_NVL_SN |
| ckupRcdSn | Long | Y | | INSP_NVL.CKUP_RCD_SN |
| schdlDt | LocalDateTime | N | 검진일(issues 2) | SCHDL.SCHDL_DT (검진일 — CKUP_RCD에 날짜 컬럼 없음, issues 1) |
| inspArtclCd | String | N | HLTG12 | INSP_NVL.INSP_ARTCL_CD |
| inspArtclCdNm | String | N | 항목 약어(예: CREA). 직접입력 항목은 null | COM_CD.COM_CD_NM, HLT-005-02, HLT-011-06·07 (추가) |
| inspArtclDrctInptNm | String | N | | INSP_NVL.INSP_ARTCL_DRCT_INPT_NM |
| rsltNvl | BigDecimal | Y | | INSP_NVL.RSLT_NVL |
| unitNm | String | Y | | INSP_NVL.UNIT_NM |
| refLlmtNvl | BigDecimal | N | | INSP_NVL.REF_LLMT_NVL |
| refUplmtNvl | BigDecimal | N | | INSP_NVL.REF_UPLMT_NVL |
| memoCn | String | N | | INSP_NVL.MEMO_CN |
| sortSeq | Integer | N | | INSP_NVL.SORT_SEQ |

### InspectionValueUpdateRequest
쓰는 API: PATCH /pets/{petSn}/inspection-values/{inspNvlSn} (검사 항목 변경 불가)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rsltNvl | BigDecimal | N | 정수 11자리·소수 4자리 | INSP_NVL.RSLT_NVL, HLT-007-02 |
| unitNm | String | N | 최대 20자. 비울 수 없음(필수 항목) | INSP_NVL.UNIT_NM, HLT-007-02 |
| refLlmtNvl | BigDecimal | N | 수정 결과 하한·상한 중 한쪽만 있으면 거부 | INSP_NVL.REF_LLMT_NVL, HLT-007-02 |
| refUplmtNvl | BigDecimal | N | 수정 결과 하한·상한 중 한쪽만 있으면 거부 | INSP_NVL.REF_UPLMT_NVL, HLT-007-02 |
| memoCn | String | N | 최대 200자 | INSP_NVL.MEMO_CN, HLT-007-02 |

### ObservationCycleCreateRequest
쓰는 API: POST /pets/{petSn}/observation-cycles (multipart/form-data, 모달 B 회차 추가)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvArtclSn | Long | Y | 이 반려동물의 관찰 항목. 종료된 항목은 불가 | OBSRV_CYCL.OBSRV_ARTCL_SN, HLT-002-19 |
| file | MultipartFile | Y | 사진 1장. jpg·jpeg·png·webp, 10MB 이하, 확장자·실제 형식 이중 검증 | OBSRV_CYCL.ATCH_FILE_SN, HLT-002-22 |
| obsrvDt | LocalDateTime | Y | 관찰일. 오늘 이후 불가. 화면은 날짜만 입력(issues 8) | OBSRV_CYCL.OBSRV_DT, HLT-002-32 |
| obsrvSz | BigDecimal | N | mm. 정수 5자리·소수 3자리 | OBSRV_CYCL.OBSRV_SZ, HLT-002-23 |
| obsrvChgCd | String | N | HLTG17 공통코드(작아짐·그대로·커짐) | OBSRV_CYCL.OBSRV_CHG_CD, HLT-002-23 |
| memoCn | String | N | 최대 200자 | OBSRV_CYCL.MEMO_CN, HLT-002-24 |

### ObservationCycleResponse
쓰는 API: PATCH /pets/{petSn}/observation-cycles/{obsrvCyclSn}; POST /pets/{petSn}/observation-cycles (ObservationCycleSummary와 같은 모양)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvCyclSn | Long | Y | 사진은 GET …/observation-cycles/{obsrvCyclSn}/photo | OBSRV_CYCL.OBSRV_CYCL_SN |
| obsrvArtclSn | Long | Y | | OBSRV_CYCL.OBSRV_ARTCL_SN |
| cyclNo | Integer | Y | 관찰일 순 회차 번호(삭제 시 다시 매김) | HLT-002-17·19 |
| obsrvDt | LocalDateTime | Y | | OBSRV_CYCL.OBSRV_DT |
| obsrvSz | BigDecimal | N | | OBSRV_CYCL.OBSRV_SZ |
| obsrvChgCd | String | N | HLTG17 | OBSRV_CYCL.OBSRV_CHG_CD |
| obsrvChgCdNm | String | N | | COM_CD.COM_CD_NM, HLT-002-15·19 |
| memoCn | String | N | | OBSRV_CYCL.MEMO_CN, HLT-002-19 |

### ObservationCycleSummary
쓰는 API: GET /pets/{petSn}/observation-cycles (관찰일 순). ObservationItemSummary의 firstCycle·lastCycle에도 씀
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvCyclSn | Long | Y | 사진은 GET …/observation-cycles/{obsrvCyclSn}/photo | OBSRV_CYCL.OBSRV_CYCL_SN, HLT-002-17 |
| obsrvArtclSn | Long | Y | | OBSRV_CYCL.OBSRV_ARTCL_SN |
| cyclNo | Integer | Y | 관찰일 순 회차 번호 | HLT-002-17·19, HLT-011-10 |
| obsrvDt | LocalDateTime | Y | | OBSRV_CYCL.OBSRV_DT, HLT-002-17 |
| obsrvSz | BigDecimal | N | 증감은 화면이 크기 있는 이전 회차와 비교해 계산 | OBSRV_CYCL.OBSRV_SZ, HLT-002-18·19 |
| obsrvChgCd | String | N | HLTG17 | OBSRV_CYCL.OBSRV_CHG_CD, HLT-002-19 |
| obsrvChgCdNm | String | N | | COM_CD.COM_CD_NM, HLT-002-19 |
| memoCn | String | N | | OBSRV_CYCL.MEMO_CN, HLT-002-19 |

### ObservationCycleUpdateRequest
쓰는 API: PATCH /pets/{petSn}/observation-cycles/{obsrvCyclSn} (multipart/form-data)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| file | MultipartFile | N | 사진 교체. jpg·jpeg·png·webp, 10MB 이하, 확장자·실제 형식 이중 검증 | OBSRV_CYCL.ATCH_FILE_SN, HLT-002-22 |
| obsrvDt | LocalDateTime | N | 오늘 이후 불가 | OBSRV_CYCL.OBSRV_DT, HLT-002-32 |
| obsrvSz | BigDecimal | N | mm | OBSRV_CYCL.OBSRV_SZ, HLT-002-23 |
| obsrvChgCd | String | N | HLTG17 | OBSRV_CYCL.OBSRV_CHG_CD, HLT-002-23 |
| memoCn | String | N | 최대 200자 | OBSRV_CYCL.MEMO_CN, HLT-002-24 |

### ObservationItemCreateRequest
쓰는 API: POST /pets/{petSn}/observation-items (multipart/form-data, 모달 A 항목 + 1회차)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvArtclNm | String | Y | 최대 40자 | OBSRV_ARTCL.OBSRV_ARTCL_NM, HLT-002-20 |
| obsrvRegnCd | String | Y | HLTG16 공통코드(머리·얼굴·목·등·배·앞다리·뒷다리·꼬리·기타) | OBSRV_ARTCL.OBSRV_REGN_CD, HLT-002-21 |
| obsrvRegnDtlNm | String | N | 최대 40자 | OBSRV_ARTCL.OBSRV_REGN_DTL_NM, HLT-002-21 |
| file | MultipartFile | Y | 1회차 사진 1장. jpg·jpeg·png·webp, 10MB 이하, 확장자·실제 형식 이중 검증 | OBSRV_CYCL.ATCH_FILE_SN, HLT-002-22 |
| obsrvDt | LocalDateTime | Y | 1회차 관찰일. 오늘 이후 불가(issues 8) | OBSRV_CYCL.OBSRV_DT, HLT-002-32 |
| obsrvSz | BigDecimal | N | mm. 1회차는 변화(obsrvChgCd) 입력 없음 | OBSRV_CYCL.OBSRV_SZ, HLT-002-23 |
| memoCn | String | N | 최대 200자 | OBSRV_CYCL.MEMO_CN, HLT-002-24 |

### ObservationItemResponse
쓰는 API: POST /pets/{petSn}/observation-items
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvArtclSn | Long | Y | 저장 후 이 항목을 선택 | OBSRV_ARTCL.OBSRV_ARTCL_SN, HLT-002-25 |
| obsrvArtclNm | String | Y | | OBSRV_ARTCL.OBSRV_ARTCL_NM |
| obsrvRegnCd | String | Y | HLTG16 | OBSRV_ARTCL.OBSRV_REGN_CD |
| obsrvRegnDtlNm | String | N | | OBSRV_ARTCL.OBSRV_REGN_DTL_NM |
| obsrvSttsCd | String | Y | HLTG18 | OBSRV_ARTCL.OBSRV_STTS_CD |
| obsrvCyclSn | Long | Y | 함께 만든 1회차 | OBSRV_CYCL.OBSRV_CYCL_SN, HLT-002-25 |

### ObservationItemSummary
쓰는 API: GET /pets/{petSn}/observation-items (첫·최근 회차 요약 포함)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| obsrvArtclSn | Long | Y | | OBSRV_ARTCL.OBSRV_ARTCL_SN |
| obsrvArtclNm | String | Y | | OBSRV_ARTCL.OBSRV_ARTCL_NM, HLT-002-15 |
| obsrvRegnCd | String | Y | HLTG16 | OBSRV_ARTCL.OBSRV_REGN_CD |
| obsrvRegnCdNm | String | Y | 부위 표시(탭 = 항목명 + 부위) | COM_CD.COM_CD_NM, HLT-002-15, HLT-011-10 |
| obsrvRegnDtlNm | String | N | | OBSRV_ARTCL.OBSRV_REGN_DTL_NM, HLT-002-15 |
| obsrvSttsCd | String | Y | HLTG18. 종료 항목은 비활성·하단 | OBSRV_ARTCL.OBSRV_STTS_CD, HLT-002-15, HLT-011-10 |
| obsrvEndYmd | LocalDate | N | | OBSRV_ARTCL.OBSRV_END_YMD, HLT-002-16 |
| cyclCnt | Integer | Y | 마지막 회차 번호([＋ 관찰 기록 추가 · N회차]의 N−1) | HLT-002-16·19 |
| lastSzDiff | BigDecimal | N | 마지막 회차 크기 − 크기를 입력한 그 이전 회차 크기. 어느 쪽이든 크기가 없으면 null | HLT-002-15·23 |
| firstCycle | ObservationCycleSummary | Y | 첫 기록 사진 비교 | HLT-011-10 |
| lastCycle | ObservationCycleSummary | Y | 대표 사진·현재 값·최근 관찰일 | HLT-002-15, HLT-011-10 |

### PrescriptionCreateRequest
쓰는 API: POST /pets/{petSn}/prescriptions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 이 반려동물의 검진기록 | PRSC.CKUP_RCD_SN, HLT-005-03 |
| mdcnNm | String | Y | 최대 40자 | PRSC.MDCN_NM, HLT-005-03 |
| mdcnBgngYmd | LocalDate | Y | | PRSC.MDCN_BGNG_YMD, HLT-005-03 |
| mdcnEndYmd | LocalDate | Y | 시작일 이후만 | PRSC.MDCN_END_YMD, HLT-005-03 |
| dayMdcnNmtm | Integer | Y | 최대 4자리. 투약 항목의 하루 목표 횟수 | PRSC.DAY_MDCN_NMTM, HLT-005-03 |
| prscSttsCd | String | Y | HLTG06 공통코드(복용중·종료) | PRSC.PRSC_STTS_CD, HLT-005-03 |
| careRfltYn | Boolean | N | 주기 체크. true면 투약 케어 항목 생성. 미전송 시 기본값은 명세에 없음(issues 13) | PRSC.CARE_RFLT_YN, HLT-005-03 |

### PrescriptionResponse
쓰는 API: PATCH /pets/{petSn}/prescriptions/{prscSn}; POST /pets/{petSn}/prescriptions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| prscSn | Long | Y | | PRSC.PRSC_SN |
| ckupRcdSn | Long | Y | | PRSC.CKUP_RCD_SN |
| mdcnNm | String | Y | | PRSC.MDCN_NM |
| mdcnBgngYmd | LocalDate | Y | | PRSC.MDCN_BGNG_YMD |
| mdcnEndYmd | LocalDate | Y | | PRSC.MDCN_END_YMD |
| dayMdcnNmtm | Integer | Y | | PRSC.DAY_MDCN_NMTM |
| prscSttsCd | String | Y | HLTG06 | PRSC.PRSC_STTS_CD |
| careRfltYn | boolean | N | | PRSC.CARE_RFLT_YN |

### PrescriptionSummary
쓰는 API: GET /pets/{petSn}/prescriptions (복용 중 우선·최신순)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| prscSn | Long | Y | | PRSC.PRSC_SN |
| ckupRcdSn | Long | Y | 행 클릭 → 그 검진상세 | PRSC.CKUP_RCD_SN, HLT-011-08 |
| mdcnNm | String | Y | | PRSC.MDCN_NM, HLT-005-03 |
| mdcnBgngYmd | LocalDate | Y | 복용 기간 | PRSC.MDCN_BGNG_YMD, HLT-005-03 |
| mdcnEndYmd | LocalDate | Y | 복용 기간 | PRSC.MDCN_END_YMD, HLT-005-03 |
| dayMdcnNmtm | Integer | Y | | PRSC.DAY_MDCN_NMTM, HLT-005-03 |
| prscSttsCd | String | Y | HLTG06 | PRSC.PRSC_STTS_CD, HLT-005-03 |
| prscSttsCdNm | String | Y | 상태 뱃지 | COM_CD.COM_CD_NM, HLT-005-03 |
| careRfltYn | boolean | N | 수정 영역 채움 | PRSC.CARE_RFLT_YN, HLT-005-03 |

### PrescriptionUpdateRequest
쓰는 API: PATCH /pets/{petSn}/prescriptions/{prscSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mdcnNm | String | N | 최대 40자. 비울 수 없음(필수 항목) | PRSC.MDCN_NM, HLT-005-03 |
| mdcnBgngYmd | LocalDate | N | 수정 결과 종료일 ≥ 시작일 | PRSC.MDCN_BGNG_YMD, HLT-005-03 |
| mdcnEndYmd | LocalDate | N | 수정 결과 종료일 ≥ 시작일. 바꾸면 투약 항목 표시 종료일도 변경 | PRSC.MDCN_END_YMD, HLT-005-03 |
| dayMdcnNmtm | Integer | N | 바꾸면 투약 항목 목표 횟수도 변경 | PRSC.DAY_MDCN_NMTM, HLT-005-03 |
| prscSttsCd | String | N | HLTG06. 종료로 바꾸면 투약 항목 삭제, 복용중으로 되돌리고 주기 체크 ON이면 새로 생성 | PRSC.PRSC_STTS_CD, HLT-005-03 |
| careRfltYn | Boolean | N | false로 바꾸면 투약 항목 삭제 | PRSC.CARE_RFLT_YN, HLT-005-03 |

### RepeatPreventionCreateRequest
쓰는 API: POST /pets/{petSn}/repeat-preventions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ckupRcdSn | Long | Y | 이 반려동물의 검진기록 | REPT_PRVNT_MNG.CKUP_RCD_SN, HLT-005-05 |
| reptPrvntArtclCd | String | Y | HLTG07 공통코드(심장사상충 예방·구충제) | REPT_PRVNT_MNG.REPT_PRVNT_ARTCL_CD, HLT-005-05 |
| reptCycleCd | String | Y | HLTG15 공통코드(1주·2주·한달·1년·직접입력) | REPT_PRVNT_MNG.REPT_CYCLE_CD, HLT-005-05 |
| cycleDrctInptDayCnt | Integer | N | 직접입력이면 필수. 최대 4자리 | REPT_PRVNT_MNG.CYCLE_DRCT_INPT_DAY_CNT, HLT-005-05 |
| bgngYmd | LocalDate | Y | 기본값 오늘. 케어 항목의 첫 예정일 | REPT_PRVNT_MNG.BGNG_YMD, HLT-005-05 |

### RepeatPreventionResponse
쓰는 API: POST /pets/{petSn}/repeat-preventions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| reptPrvntMngSn | Long | Y | | REPT_PRVNT_MNG.REPT_PRVNT_MNG_SN |
| ckupRcdSn | Long | Y | | REPT_PRVNT_MNG.CKUP_RCD_SN |
| reptPrvntArtclCd | String | Y | HLTG07 | REPT_PRVNT_MNG.REPT_PRVNT_ARTCL_CD |
| reptCycleCd | String | Y | HLTG15 | REPT_PRVNT_MNG.REPT_CYCLE_CD |
| cycleDrctInptDayCnt | Integer | N | | REPT_PRVNT_MNG.CYCLE_DRCT_INPT_DAY_CNT |
| bgngYmd | LocalDate | Y | | REPT_PRVNT_MNG.BGNG_YMD |
| nxtmPrnmntYmd | LocalDate | N | 자동 추가한 케어 항목의 다음 예정일 | CARE_ARTCL.NXTM_PRNMNT_YMD, HLT-005-05 |

### RepeatPreventionSummary
쓰는 API: GET /pets/{petSn}/repeat-preventions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| reptPrvntMngSn | Long | Y | [해지] 대상 | REPT_PRVNT_MNG.REPT_PRVNT_MNG_SN, HLT-005-05 |
| ckupRcdSn | Long | Y | | REPT_PRVNT_MNG.CKUP_RCD_SN |
| reptPrvntArtclCd | String | Y | HLTG07 | REPT_PRVNT_MNG.REPT_PRVNT_ARTCL_CD |
| reptPrvntArtclCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-005-05 |
| reptCycleCd | String | Y | HLTG15 | REPT_PRVNT_MNG.REPT_CYCLE_CD |
| reptCycleCdNm | String | Y | | COM_CD.COM_CD_NM, HLT-005-05 |
| cycleDrctInptDayCnt | Integer | N | | REPT_PRVNT_MNG.CYCLE_DRCT_INPT_DAY_CNT |
| bgngYmd | LocalDate | Y | | REPT_PRVNT_MNG.BGNG_YMD, HLT-005-05 |
| nxtmPrnmntYmd | LocalDate | N | 목록 행의 다음 예정일 | CARE_ARTCL.NXTM_PRNMNT_YMD, HLT-005-05 |
