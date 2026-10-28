# 알림(notification) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

이 도메인의 API는 모두 구현됐다(2026-10-22 구현에 맞춤). 정본은 Swagger다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /members/me/notification-settings | 알림 수신 설정 조회(채널·유형·사전 알림 시점·한도 설정 여부) | COM-008-02·03·05~10 | LJS | - | NotificationSettingResponse |
| GET | /notifications | 알림 드롭다운 최대 5건(안 읽음 우선, 같은 상태끼리 최신순, 탭 필터, 7일 경과 숨김 제외, 조회만으로 읽음 처리 안 함) `?tab=ALL` | COM-004-03~05·11·12 | LJS | - | List<NotificationSummary> |
| GET | /notifications/unread-count | 미읽음 알림 수(헤더 배지·드롭다운 헤더) | COM-002-09, COM-004-02 | LJS | - | UnreadCountResponse |
| POST | /notifications/{inappNotiSn}/read | 알림 개별 읽음 처리. 남의·없는 알림은 404 `NOTIFICATION_NOT_FOUND` | COM-004-06 | LJS | - | - |
| PUT | /members/me/notification-settings | 알림 수신 설정 일괄 저장. 케어 일정 ON에 `leadTimes`가 비면 400 `INVALID_INPUT_VALUE` | COM-008-02·05~09·14·20 | LJS | NotificationSettingUpdateRequest | NotificationSettingResponse |

## DTO

### NotificationSettingResponse
쓰는 API: GET /members/me/notification-settings; PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| maskedAcntEml | String | Y | 마스킹한 가입 이메일, 변경 불가 표시 | MBR.ACNT_EML_ENC, COM-008-03 |
| emlNotiRcptnYn | boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| leadTimes | List<String> | Y | 사전 알림 시점. `TODAY`·`D1`·`D3`·`D7` 중 켜진 것. 없으면 빈 목록 | MBR.THDY·D1·D3·D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |
| expndLimSetYn | boolean | Y | 월 지출 한도 설정 여부(계산) | MBR.MM_EXPND_LIM_AMT 존재 여부, COM-008-10 |

### NotificationSettingUpdateRequest
쓰는 API: PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlNotiRcptnYn | Boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | Boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | Boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| leadTimes | List<String> | Y | `TODAY`·`D1`·`D3`·`D7` 복수 선택. 케어 일정 ON이면 1개 이상 | MBR.THDY·D1·D3·D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | Boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | Boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | Boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | Boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |

### NotificationSummary
쓰는 API: GET /notifications (목록 행. 안 읽음 우선·같은 상태끼리 최신순, 7일 경과 숨김 제외)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inappNotiSn | Long | Y | | INAPP_NOTI.INAPP_NOTI_SN |
| notiTypeCd | String | Y | AIMG02 공통코드 | INAPP_NOTI.NOTI_TYPE_CD, COM-004-04 |
| tab | String | Y | 행의 탭(`CARE`·`COMMUNITY`·`INFO`). 유형 라벨은 화면이 정한다 | COM-004-04 |
| notiTtl | String | Y | | INAPP_NOTI.NOTI_TTL |
| notiCn | String | Y | | INAPP_NOTI.NOTI_CN |
| mvmnPath | String | N | 유형별 이동 대상 | INAPP_NOTI.MVMN_PATH, COM-004-07 |
| prslDt | LocalDateTime | N | null이면 미읽음 | INAPP_NOTI.PRSL_DT, COM-004-06·11 |
| regDt | LocalDateTime | Y | 받은 시각 | INAPP_NOTI.REG_DT |

### UnreadCountResponse
쓰는 API: GET /notifications/unread-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| count | Long | Y | 헤더 배지(0이면 숨김, 99 초과 '99+')·드롭다운 헤더 | INAPP_NOTI.PRSL_DT IS NULL 건수, COM-002-09, COM-004-02 |
