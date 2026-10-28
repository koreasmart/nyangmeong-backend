# 가계부(expense) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /expenses | 지출 목록. 쿼리 `startYmd`·`endYmd`(월 조회·편집 모드 삭제 범위)·`petSn`·`expndClsfCd`·`sort`(최신순/금액순)·`page`(20건). 날짜·금액·카테고리·반려동물 일치 조건으로 중복 지출 확인에도 씀 | MYP-030-03·09~12·17·20, MYP-031-06 | SJM | - | Page<ExpenseSummary> |
| GET | /expenses/months | 지출이 한 건 이상 있는 연월 목록(월 피커) | MYP-030-16 | SJM | - | List<ExpenseMonthSummary> |
| GET | /expenses/statistics | 월 통계. 쿼리 `yearMonth`. 총 지출·월 한도·지난달 대비·일자별 합계와 건수·카테고리별·반려동물별(공용 별도)·금액 상위 3건 | MYP-001-03, MYP-030-01·02·04~06·14 | SJM | - | ExpenseStatisticsResponse |
| GET | /expenses/totals | 조건별 건수·합계 금액. 쿼리는 GET /expenses와 같음(페이지 제외) | MYP-030-09·17~19 | SJM | - | ExpenseTotalResponse |
| POST | /expenses | 지출 등록. 날짜·카테고리·내용 공통, 반려동물·금액 줄 여러 개(영수증 마리별 분할), 영수증 연결(`recptSn`) | MYP-031-02·03 | SJM | ExpenseCreateRequest | List<ExpenseResponse> |
| PUT | /members/me/expense-limit | 월 지출 한도 저장(계정 전체 한 개). 처음 저장하면 한도 알림 수신을 켬 | MYP-030-13 | SJM | ExpenseLimitSaveRequest | ExpenseLimitResponse |
| PATCH | /expenses/{expndSn} | 지출 수정 | MYP-030-03·12 | SJM | ExpenseUpdateRequest | ExpenseResponse |
| DELETE | /expenses | 지출 다중 삭제(개별 선택). `?expndSn=1&expndSn=2`, 1개 이상. 본문 없음(`docs/convention/api.md`) | MYP-030-19·21 | SJM | - | - |
| POST | /expenses/bulk-delete | 지출 조건 전체 삭제([조회 결과 전체 선택]). 범위·필터에 맞는 전체 건. 200, `data` 없음 | MYP-030-18·19·21 | SJM | ExpenseBulkDeleteRequest | - |
| DELETE | /expenses/{expndSn} | 지출 한 건 삭제 | MYP-030-03·15 | SJM | - | - |

## DTO

### ExpenseBulkDeleteRequest
쓰는 API: POST /expenses/bulk-delete (개별 선택 삭제는 `DELETE /expenses?expndSn=`)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expectedCnt | Integer | Y | 화면이 보여 준 삭제 건수(`GET /expenses/totals`). 서버가 센 건수와 다르면 400으로 거절해, 그 사이 바뀐 조건으로 지우지 않는다 | MYP-030-18·19 (issues 8번) |
| startYmd | LocalDate | N | 삭제 범위 시작 월의 첫날. 종료보다 뒤면 서로 바꿔 적용. `endYmd`와 함께 생략하면 전체 기간 | MYP-030-17 |
| endYmd | LocalDate | N | 삭제 범위 종료 월의 말일 | MYP-030-17 |
| petSn | Long | N | 반려동물 필터. 생략하면 전체("공용" 표현은 issues 8번) | EXPND.PET_SN, MYP-030-10·17 |
| expndClsfCd | String | N | 카테고리 필터(MYPG05). 생략하면 전체 | EXPND.EXPND_CLSF_CD, MYP-030-10·17 |

### ExpenseCreateRequest
쓰는 API: POST /expenses
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndYmd | LocalDate | Y | 모든 줄에 공통 적용 | EXPND.EXPND_YMD |
| expndClsfCd | String | Y | 카테고리 7종(MYPG05). 모든 줄에 공통 적용 | EXPND.EXPND_CLSF_CD |
| dtlMttr | String | N | 최대 1000자. 모든 줄에 공통 적용 | EXPND.DTL_MTTR |
| expndPlcusNm | String | N | 사용처. 최대 80자. 모든 줄에 공통 적용. 영수증 등록이면 상호명이 기본값 | EXPND.EXPND_PLCUS_NM (`db/migration/20261025_01_add_expnd_plcus_nm.sql`), MYP-031-02·03 |
| recptSn | Long | N | 영수증 등록이면 그 영수증. 모든 줄을 같은 영수증에 연결 | EXPND.RECPT_SN, MYP-031-03·05 |
| expndInptMthCd | String | Y | 직접 입력·영수증 구분(MYPG06) | EXPND.EXPND_INPT_MTH_CD |
| lines | List<ExpenseLineRequest> | Y | 1줄 이상. 같은 반려동물을 두 줄에 고를 수 없음. 줄 수는 연동된 반려동물 수까지 | MYP-031-03 |

#### ExpenseLineRequest (ExpenseCreateRequest.lines)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | N | 비우면 공용. 병원 카테고리면 필수(공용 불가). 연동된 반려동물만 | EXPND.PET_SN, MYP-031-02·03 |
| expndAmt | Long | Y | 1 이상(0원 불가) | EXPND.EXPND_AMT, MYP-031-02·03 |
| idmptDmndId | String | Y | 줄마다 클라이언트가 만든 UUID, 최대 100자. 중복 클릭·재전송 차단. 모든 줄의 값이 이미 있으면 200과 기존 지출, 처음이면 201. 일부만 있으면 409 `ALREADY_PROCESSED` | EXPND.IDMPT_DMND_ID (`db/migration/20261022_01_add_expnd_idmpt_dmnd_id.sql`) |

### ExpenseLimitResponse
쓰는 API: PUT /members/me/expense-limit
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mmExpndLimAmt | Long | Y | | MBR.MM_EXPND_LIM_AMT |
| expndLimNotiRcptnYn | boolean | Y | 처음 저장하면 `true`로 켜짐 | MBR.EXPND_LIM_NOTI_RCPTN_YN, MYP-030-13 |

### ExpenseLimitSaveRequest
쓰는 API: PUT /members/me/expense-limit
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mmExpndLimAmt | Long | Y | 최대 13자리 정수(컬럼). 계정 전체 한도 하나 | MBR.MM_EXPND_LIM_AMT, MYP-030-13 |

### ExpenseMonthSummary
쓰는 API: GET /expenses/months
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| yearMonth | String | Y | 지출이 한 건 이상 있는 연월. 형식 `yyyy-MM`(issues 9번) | MYP-030-16, EXPND.EXPND_YMD |

### ExpenseResponse
쓰는 API: PATCH /expenses/{expndSn}; POST /expenses
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndSn | Long | Y | | EXPND.EXPND_SN |
| petSn | Long | N | null이면 공용 | EXPND.PET_SN |
| petNm | String | N | | PET.PET_NM |
| expndClsfCd | String | Y | | EXPND.EXPND_CLSF_CD |
| expndAmt | Long | Y | | EXPND.EXPND_AMT |
| dtlMttr | String | N | | EXPND.DTL_MTTR |
| expndPlcusNm | String | N | 사용처 | EXPND.EXPND_PLCUS_NM |
| expndYmd | LocalDate | Y | | EXPND.EXPND_YMD |
| recptSn | Long | N | | EXPND.RECPT_SN |

### ExpenseStatisticsResponse
쓰는 API: GET /expenses/statistics
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| totalExpndAmt | Long | Y | 선택한 달의 총 지출액 | MYP-030-01, MYP-001-03 |
| mmExpndLimAmt | Long | N | null이면 한도 미설정(안내로 대체) | MBR.MM_EXPND_LIM_AMT, MYP-030-01·14 |
| lastMonthTotalExpndAmt | Long | N | 지난달 총 지출. 비교 대상이 없는 첫 달이면 null | MYP-030-01 |
| days | List<ExpenseStatisticsDayItem> | Y | 지출이 있는 날짜만 | MYP-030-02 |
| categories | List<ExpenseStatisticsCategoryItem> | Y | 비율은 `totalExpndAmt`로 프론트가 계산 | MYP-030-04 |
| pets | List<ExpenseStatisticsPetItem> | Y | 반려동물 등록순(issues 10번). 공용은 제외 | MYP-030-05 |
| commonTotalExpndAmt | Long | Y | 공용 지출 합계. 없으면 0 | MYP-030-05 |
| topExpenses | List<ExpenseSummary> | Y | 금액 큰 개별 지출 최대 3건 | MYP-030-06 |

#### ExpenseStatisticsDayItem (ExpenseStatisticsResponse.days)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndYmd | LocalDate | Y | | EXPND.EXPND_YMD |
| totalExpndAmt | Long | Y | 그날 합계 | MYP-030-02 |
| expndCnt | Integer | Y | 그날 건수 | MYP-030-02 |
| petSn | Long | N | 그날 1건일 때만. 공용이면 null | EXPND.PET_SN, MYP-030-02 |
| petNm | String | N | 그날 1건일 때만 | PET.PET_NM, MYP-030-02 |
| expndClsfCd | String | N | 그날 1건일 때만 | EXPND.EXPND_CLSF_CD, MYP-030-02 |

#### ExpenseStatisticsCategoryItem (ExpenseStatisticsResponse.categories)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndClsfCd | String | Y | | EXPND.EXPND_CLSF_CD |
| totalExpndAmt | Long | Y | | MYP-030-04 |

#### ExpenseStatisticsPetItem (ExpenseStatisticsResponse.pets)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | EXPND.PET_SN |
| petNm | String | Y | | PET.PET_NM |
| totalExpndAmt | Long | Y | 비율은 프론트가 계산 | MYP-030-05 |

### ExpenseSummary
쓰는 API: GET /expenses
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndSn | Long | Y | | EXPND.EXPND_SN |
| expndYmd | LocalDate | Y | 날짜 그룹·요일은 프론트가 계산 | EXPND.EXPND_YMD, MYP-030-12 |
| expndClsfCd | String | Y | 카테고리 배지 | EXPND.EXPND_CLSF_CD, MYP-030-12 |
| petSn | Long | N | null이면 공용 | EXPND.PET_SN |
| petNm | String | N | | PET.PET_NM, MYP-030-12 |
| petDelYn | boolean | N | `true`면 "(삭제된 반려동물)" 표기. 공용이면 null | PET.DEL_YN, MYP-002-07 (issues 12번) |
| dtlMttr | String | N | 내용 | EXPND.DTL_MTTR, MYP-030-12 |
| expndPlcusNm | String | N | 사용처 | EXPND.EXPND_PLCUS_NM, MYP-031-06 |
| expndAmt | Long | Y | | EXPND.EXPND_AMT, MYP-030-12 |
| recptSn | Long | N | | EXPND.RECPT_SN |

### ExpenseTotalResponse
쓰는 API: GET /expenses/totals
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndCnt | Integer | Y | 조건에 맞는 건수 | MYP-030-09·19 |
| totalExpndAmt | Long | Y | 조건에 맞는 합계 금액 | MYP-030-09·19 |

### ExpenseUpdateRequest
쓰는 API: PATCH /expenses/{expndSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| expndYmd | LocalDate | N | | EXPND.EXPND_YMD, MYP-030-12 |
| expndClsfCd | String | N | 카테고리 7종(MYPG05). 병원으로 바꾸면 반려동물 필수 | EXPND.EXPND_CLSF_CD, MYP-031-02 |
| petSn | Long | N | 연동된 반려동물만. 공용으로 바꾸는 방법은 issues 4번 | EXPND.PET_SN, MYP-031-02 |
| expndAmt | Long | N | 1 이상(수정 시에도 0원 불가) | EXPND.EXPND_AMT, MYP-031-02 |
| dtlMttr | String | N | 최대 1000자. `""`이면 비움 | EXPND.DTL_MTTR |
| expndPlcusNm | String | N | 최대 80자. `""`이면 비움 | EXPND.EXPND_PLCUS_NM |
