# AI 대화(ai) 내부 API·DTO

공통 규칙(응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 Python AI 서버가 호출하는 대화 저장·조회 내부 API만 담는다. 구현된 API의 정본은 Swagger(`/swagger-ui.html`)다.

## 호출 규칙

- 경로는 `/api` 접두어가 없는 `/internal/ai/conversations` 아래다(`/internal/auth/check`와 같다). nginx 밖에서만 부른다.
- 회원은 쿠키로 판정한다(2026-10-27 ai 저장소 합의). Python은 React에서 받은 `Cookie` 헤더(`access_token`)를 그대로 실어 부르고, 요청에 `mbrSn`을 넣지 않는다. 회원 번호를 받으면 서버 간 키가 없는 상태에서 누구나 남의 대화를 읽고 지울 수 있다.
- 인증 실패는 기존 필터와 같다: 세션 폐기 401 `UNAUTHORIZED`, 정지·탈퇴 403 `MEMBER_INACTIVE`(쿠키 삭제 `Set-Cookie`). Python은 이 본문을 그대로 React에 넘긴다.
- 상태를 바꾸는 요청(POST·DELETE)은 `X-Requested-With: XMLHttpRequest` 헤더가 필요하다(`RequestedWithFilter`). Python은 React의 헤더를 같이 넘긴다.
- 제목·메시지 내용은 서버가 `app.crypto.aes-key-base64`로 암호화해 저장하고 응답에서 복호화한다. Python은 평문만 다룬다.
- 남의 대화나 없는 번호는 같은 404 `NOT_FOUND`다. 없는 반려동물 `petSn`과 양육하지 않는 반려동물 `petSn`도 존재 여부를 알려 주지 않으려고 같은 404 `NOT_FOUND`다(생성·목록 조회 공통).
- `roleCd`는 공통코드 AIMG06이다: `AIMC0024` 사용자, `AIMC0025` AI.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| POST | /internal/ai/conversations | 대화 생성(방과 첫 메시지를 한 트랜잭션으로 저장, 201) | AIM-011-07·11·14 | LJS | ConversationCreateRequest | ConversationSummary |
| GET | /internal/ai/conversations?petSn= | 대화 목록(최신순). petSn을 주면 그 태그만(태그 없는 「전체」 대화 제외) | AIM-011-08·09 | LJS | - | List<ConversationSummary> |
| GET | /internal/ai/conversations/{aiCnvrsRoomSn} | 대화 상세(메시지 `msgSeq` 오름차순) | AIM-011-10 | LJS | - | ConversationResponse |
| POST | /internal/ai/conversations/{aiCnvrsRoomSn}/messages | 메시지 추가(사용자·AI, 순서는 서버가 매김, 201) | AIM-011-10·14 | LJS | ConversationMessageCreateRequest | ConversationMessageResponse |
| DELETE | /internal/ai/conversations/{aiCnvrsRoomSn} | 대화 삭제(물리 삭제, 메시지 CASCADE, 200) | AIM-011-13 | LJS | - | - |

탈퇴 회원 대화 파기(AIM-011-19)는 API가 아니라 `scheduler/AiConversationPurgeJob`이 매일 03:00 KST에 한다. Python의 `DELETE /internal/v1/members/{mbrSn}/conversations`는 쓰지 않는다.

후속 설계(이번에 만들지 않음): 미완료(`cmptnYn=false`) 메시지를 완료로 바꾸는 `PATCH /internal/ai/conversations/{aiCnvrsRoomSn}/messages/{aiCnvrsMsgSn}` {cmptnYn, cnvrsCn?}. SCR-AIM-002·012에서 필요해지면 추가한다.

## DTO

### ConversationCreateRequest
쓰는 API: POST /internal/ai/conversations
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | N | null이면 「전체」 대화. 값이 있으면 양육자만 | AI_CNVRS_ROOM.PET_SN, AIM-011-08 |
| cnvrsTtl | String | Y | 1~200자, 서버가 암호화 | AI_CNVRS_ROOM.CNVRS_TTL_ENC, AIM-011-11 |
| cnvrsCn | String | Y | 1~1000자, 첫 메시지(사용자 역할, `msgSeq` 1) | AI_CNVRS_MSG.CNVRS_CN_ENC, AIM-011-07 |

### ConversationMessageCreateRequest
쓰는 API: POST /internal/ai/conversations/{aiCnvrsRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| roleCd | String | Y | `AIMC0024`·`AIMC0025` | AI_CNVRS_MSG.ROLE_CD |
| cnvrsCn | String | Y | 1~1000자 | AI_CNVRS_MSG.CNVRS_CN_ENC |
| cmptnYn | Boolean | N | 기본 true. false면 미완료 답변 | AI_CNVRS_MSG.CMPTN_YN |

### ConversationSummary
쓰는 API: POST·GET /internal/ai/conversations
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiCnvrsRoomSn | Long | Y | | AI_CNVRS_ROOM.AI_CNVRS_ROOM_SN |
| petSn | Long | N | 태그 없으면 null | AI_CNVRS_ROOM.PET_SN |
| cnvrsTtl | String | Y | 복호화한 제목 | AI_CNVRS_ROOM.CNVRS_TTL_ENC |
| regDt | LocalDateTime | Y | | AI_CNVRS_ROOM.REG_DT |

### ConversationResponse
쓰는 API: GET /internal/ai/conversations/{aiCnvrsRoomSn}. ConversationSummary 필드 + 아래.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| messages | List<ConversationMessageResponse> | Y | `msgSeq` 오름차순 | AI_CNVRS_MSG |

### ConversationMessageResponse
쓰는 API: GET 상세, POST .../messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiCnvrsMsgSn | Long | Y | | AI_CNVRS_MSG.AI_CNVRS_MSG_SN |
| roleCd | String | Y | `AIMC0024`·`AIMC0025` | AI_CNVRS_MSG.ROLE_CD |
| cnvrsCn | String | Y | 복호화한 내용 | AI_CNVRS_MSG.CNVRS_CN_ENC |
| msgSeq | Integer | Y | 대화 안의 순서(1부터) | AI_CNVRS_MSG.MSG_SEQ |
| cmptnYn | boolean | Y | 완료 여부 | AI_CNVRS_MSG.CMPTN_YN |
| regDt | LocalDateTime | Y | | AI_CNVRS_MSG.REG_DT |
