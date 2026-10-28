# 커뮤니티(community) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /care-matches/{petPrtcMtchSn} | 매칭 조건·완료 요청 회차·사진·수령 상태 조회 | CMU-005-09·11 | KSJ | - | CareMatchResponse |
| GET | /care-recruitments | 펫돌봄 모집 목록(상태·지역·기간·동물 구분·크기 필터, 10건) | CMU-004-01·02 | KSJ | - | Page<CareRecruitmentSummary> |
| GET | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 상세(상세 주소·정확 좌표 제외) | CMU-004-04 | KSJ | - | CareRecruitmentResponse |
| GET | /chat-rooms | 내 채팅 목록(`?unreadOnly=`) | CMU-005-02, COM-009 | KSJ | - | List<ChatRoomSummary> |
| GET | /chat-rooms/unread-count | 읽지 않은 메시지 총수(채팅 메뉴·헤더 배지) | CMU-005-03, COM-009 | KSJ | - | ChatUnreadCountResponse |
| GET | /chat-rooms/{otoChatRoomSn} | 채팅방 상단 게시글 카드·매칭 진행 상태·내 역할 조회(참여자만) | CMU-005-01·11, COM-009 | KSJ | - | ChatRoomResponse |
| GET | /chat-rooms/{otoChatRoomSn}/messages | 대화 메시지 조회(커서 방식, 최신순). `?beforeChatMsgSn=&size=30` — 처음은 `beforeChatMsgSn` 없이, 위로 스크롤하면 받은 것 중 가장 오래된 `chatMsgSn`을 넘긴다. `size` 최대 50 | CMU-005-04, COM-009 | KSJ | - | ChatMessageSliceResponse |
| GET | /community-ai-commentaries | 오늘의 이야기 조회(`?cmtryCrtrYmd=`) | CMU-001-05 | KSJ | - | CommunityAiCommentaryResponse |
| GET | /lost-reports | 실종 제보 목록(상태·지역·동물 구분·최종 목격 기간·키워드, 12건) | CMU-002-01~04 | KSJ | - | Page<LostReportSummary> |
| GET | /lost-reports/{lostTpofSn} | 실종 제보 상세(목격 장소 좌표, 찾았어요 후 비공개 처리) | CMU-002-05·06 | KSJ | - | LostReportResponse |
| GET | /news | 펫뉴스 목록(카테고리·키워드, 10건) | CMU-003-01~04 | KSJ | - | Page<NewsSummary> |
| GET | /news/{newsSn} | 펫뉴스 상세 | CMU-003-04 | KSJ | - | NewsResponse |
| GET | /posts | 펫이야기 목록(게시판·정렬·키워드, 인기글 배지, 10건). 상세 하단 최신 5건도 사용 | CMU-001-01~04·08·10 | KSJ | - | Page<PostSummary> |
| GET | /posts/health-tips | 오늘의 건강팁(관리자 선정 3건) | CMU-001-07 | KSJ | - | List<PostSummary> |
| GET | /posts/popular-photo | 인기 포토(7일 → 30일 → 전체 순 대체) | CMU-001-06 | KSJ | - | PostResponse |
| GET | /posts/{pstSn} | 게시글 상세(조회수 동일 사용자 1일 1회 증가) | CMU-001-10 | KSJ | - | PostResponse |
| GET | /posts/{pstSn}/comments | 댓글·대댓글 목록(삭제 댓글 대체 문구) | CMU-001-11, CMU-002-07 | KSJ | - | List<CommentSummary> |
| POST | /care-matches | 매칭 요청(채팅방 기준, 조건 버전 생성) | CMU-005-06 | KSJ | CareMatchCreateRequest | CareMatchResponse |
| POST | /care-matches/{petPrtcMtchSn}/accept | 매칭 수락(게시글당 1명 확정, 모집 상태 갱신) | CMU-005-07 | KSJ | - | - |
| POST | /care-matches/{petPrtcMtchSn}/cancel | 수락 전 매칭 요청 취소 | CMU-005-06 | KSJ | - | - |
| POST | /care-matches/{petPrtcMtchSn}/completion-requests | 돌봄 완료 요청(회차·종료 시각·내용·특이사항·인계 확인·사진) | CMU-005-08 | KSJ | CareCompletionRequestCreateRequest | CareCompletionRequestResponse |
| POST | /care-matches/{petPrtcMtchSn}/confirm-completion | 보호자 돌봄 완료 확정 | CMU-005-09 | KSJ | - | - |
| POST | /care-matches/{petPrtcMtchSn}/confirm-receipt | 제공자 비용 수령 확인(양측 충족 시 거래 완료) | CMU-005-10 | KSJ | CareMatchReceiptConfirmRequest | - |
| POST | /care-matches/{petPrtcMtchSn}/request-supplement | 보호자 보완 요청(사유, 확인 대기 유지) | CMU-005-09 | KSJ | CareMatchSupplementRequest | - |
| POST | /care-recruitments | 펫돌봄 모집 등록(반려동물·돌봄 형태·일정·비용 최저시급 검증·장소·사진·동의) | CMU-004-06~11·13 | KSJ | CareRecruitmentCreateRequest | CareRecruitmentResponse |
| POST | /chat-rooms | 게시글·보호자·지원자 조합 채팅방 생성 또는 기존 방 반환 | CMU-004-05, CMU-005-01 | KSJ | ChatRoomCreateRequest | ChatRoomResponse |
| POST | /chat-rooms/{otoChatRoomSn}/messages | 메시지 전송(텍스트·사진·주소 공유, 멱등 요청 ID). 처음은 201, 같은 `idmptDmndId` 재전송은 200과 같은 응답. 저장 뒤 참여자에게 실시간으로 보낸다(아래 "실시간") | CMU-004-05, CMU-005-04·05, COM-009 | KSJ | ChatMessageCreateRequest | ChatMessageResponse |
| POST | /chat-rooms/{otoChatRoomSn}/read | 활성 대화창에 표시된 메시지까지 읽음 처리 | CMU-005-03, COM-009 | KSJ | ChatRoomReadRequest | - |
| POST | /lost-reports | 실종 제보 등록(게시글+제보, 품종 마스터/직접입력, 목격 좌표, 개인정보 동의) | CMU-002-10·13 | KSJ | LostReportCreateRequest | LostReportResponse |
| POST | /lost-reports/{lostTpofSn}/found | 찾았어요 전환(완료 안내 댓글 자동 생성) | CMU-002-09 | KSJ | - | - |
| POST | /lost-reports/{lostTpofSn}/renew | 만료 전 게시 기간 갱신 | CMU-002-09 | KSJ | - | - |
| POST | /posts | 펫이야기 게시글 작성(사진 최대 10장) | CMU-001-14 | KSJ | PostCreateRequest | PostResponse |
| POST | /posts/{pstSn}/comments | 댓글·대댓글(1depth) 등록 | CMU-001-11, CMU-002-07 | KSJ | CommentCreateRequest | CommentResponse |
| POST | /posts/{pstSn}/likes | 좋아요 등록. 멱등: 이미 누른 상태여도 200과 현재 좋아요 수(`UK_PST_RCMDTN_1`로 중복 행을 막는다) | CMU-001-09, CMU-002-08, CMU-004-12 | KSJ | - | PostLikeResponse |
| POST | /reports | 게시글·댓글 신고(사유·상세, 중복 신고 제한, 누적 처리) | CMU-001-13, CMU-002-12, CMU-004-12 | KSJ | ReportCreateRequest | ReportResponse |
| PATCH | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 수정(작성자) | CMU-004-11 | KSJ | CareRecruitmentUpdateRequest | CareRecruitmentResponse |
| PATCH | /posts/{pstSn} | 게시글 수정(작성자) | CMU-001-15 | KSJ | PostUpdateRequest | PostResponse |
| DELETE | /care-recruitments/{petPrtcRcrtSn} | 펫돌봄 모집 삭제(작성자, 매칭·채팅 기록 유지) | CMU-004-11 | KSJ | - | - |
| DELETE | /posts/{pstSn} | 게시글 삭제(작성자) | CMU-001-15 | KSJ | - | - |
| DELETE | /posts/{pstSn}/comments/{cmntSn} | 댓글 삭제(답글 유지) | CMU-001-11, CMU-002-07 | KSJ | - | - |
| DELETE | /posts/{pstSn}/likes | 좋아요 취소. 멱등: 누르지 않은 상태여도 200 | CMU-001-09, CMU-002-08, CMU-004-12 | KSJ | - | - |

## 실시간 (채팅, 2026-10-22 제안)

REST는 보내기·과거 조회·읽음 처리만 맡고, 새 메시지와 읽음 변경은 STOMP(WebSocket)로 받는다. 보내기를 REST로 두는 이유는 `idmptDmndId` 멱등과 `ApiResponse` 오류 응답을 그대로 쓰기 위해서다.

| 항목 | 값 |
| --- | --- |
| 연결 | `/ws` (STOMP over WebSocket). 연결할 때 access 토큰 쿠키로 인증하고, 연결 속성에 `mbrSn`·`sid`(RFRSH_TKN 일련번호)를 저장한다 |
| 구독 | `/user/queue/chat`. 사용자 식별자는 `mbrSn`이라 같은 회원의 모든 기기 연결로 간다 |
| 받는 것 | `{ type: "MESSAGE", otoChatRoomSn, message: ChatMessageSummary }`, `{ type: "READ", otoChatRoomSn, mbrSn, msgSeq }` |

다중 로그인(한 회원이 여러 기기로 동시에 접속)에서 지킬 것:
- 메시지와 읽음은 회원의 모든 연결로 보낸다. 읽음 위치는 DB(회원 + 방 기준 마지막 읽은 `msgSeq`)에 두고, 한 기기에서 읽으면 다른 기기 연결에도 `READ`를 보낸다. 헤더의 채팅 안 읽은 수(`GET /chat-rooms/unread-count`)도 같은 값을 쓴다
- "채팅방을 보는 중이면 거래 알림을 만들지 않는다"(COM-004-14)는 그 회원의 연결 중 하나라도 그 방을 보고 있으면 보는 중으로 본다. 기준(연결 여부 / 화면 포커스)은 KSJ가 정한다
- 로그아웃·세션 폐기 뒤에도 소켓이 살아 있으면 안 된다. `LoginSessionManager.revoke`가 세션 폐기 이벤트를 발행하고, 채팅 쪽이 그 `sid`의 연결을 끊는다. 오래 열린 연결은 access 토큰 만료 때 다시 인증한다
- 서버가 한 대면 내장 브로커로 충분하다. 여러 대가 되면 외부 브로커(Redis pub/sub 등)가 필요하다

## DTO

### CareCompletionRequestCreateRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/completion-requests
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| prtcEndDt | LocalDateTime | Y | 돌봄 종료 시각 | PRTC_CMPTN_DMND_CYCL.PRTC_END_DT, CMU-005-08 |
| cmptnCn | String | Y | 돌봄 내용. 최대 4000자 | PRTC_CMPTN_DMND_CYCL.CMPTN_CN, CMU-005-08 |
| spclMttrCn | String | N | 특이사항. 최대 1000자 | PRTC_CMPTN_DMND_CYCL.SPCL_MTTR_CN, CMU-005-08 |
| hnovCmptnIdntyYn | Boolean | Y | 인계·마무리 확인. true여야 제출 | PRTC_CMPTN_DMND_CYCL.HNOV_CMPTN_IDNTY_YN, CMU-005-08 |
| atchFileSns | List<Long> | N | 사진 선택 첨부. 개수·용량은 명세 없음 | PRTC_CMPTN_PHOTO.ATCH_FILE_SN, CMU-005-08 |
| idmptDmndId | String | Y | 중복 제출 방지 | PRTC_CMPTN_DMND_CYCL.IDMPT_DMND_ID, CMU-005-11 |

### CareCompletionRequestResponse
쓰는 API: POST /care-matches/{petPrtcMtchSn}/completion-requests (`CareMatchResponse.cmptnDmnds`에서도 씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| prtcCmptnDmndCyclSn | Long | Y | | PRTC_CMPTN_DMND_CYCL.PRTC_CMPTN_DMND_CYCL_SN |
| petPrtcMtchSn | Long | Y | | PRTC_CMPTN_DMND_CYCL.PET_PRTC_MTCH_SN |
| mtchCndVerSn | Long | Y | 요청 당시 조건 버전 | PRTC_CMPTN_DMND_CYCL.MTCH_CND_VER_SN |
| dmndCycl | Integer | Y | 요청 회차 | PRTC_CMPTN_DMND_CYCL.DMND_CYCL, CMU-005-09 |
| prtcEndDt | LocalDateTime | Y | | PRTC_CMPTN_DMND_CYCL.PRTC_END_DT |
| cmptnCn | String | Y | | PRTC_CMPTN_DMND_CYCL.CMPTN_CN |
| spclMttrCn | String | N | | PRTC_CMPTN_DMND_CYCL.SPCL_MTTR_CN |
| hnovCmptnIdntyYn | boolean | Y | | PRTC_CMPTN_DMND_CYCL.HNOV_CMPTN_IDNTY_YN |
| prtcrIdntyRsltCd | String | N | 보호자 확인 결과. 공통코드 PCAG06. 확인 전 null | PRTC_CMPTN_DMND_CYCL.PRTCR_IDNTY_RSLT_CD, CMU-005-09 |
| prtcrIdntyDt | LocalDateTime | N | | PRTC_CMPTN_DMND_CYCL.PRTCR_IDNTY_DT, CMU-005-11 |
| splmntDmndRsn | String | N | 보완 요청 사유 | PRTC_CMPTN_DMND_CYCL.SPLMNT_DMND_RSN, CMU-005-09 |
| photos | List<ImageItem> | Y | 없으면 빈 목록 | PRTC_CMPTN_PHOTO, CMU-005-09 |

#### ImageItem (응답 record. 펫이야기는 접미어 규칙에 맞춰 `PostImageSummary`로 쓴다. 쓰는 DTO: CareCompletionRequestResponse, CareRecruitmentResponse, ChatMessageResponse, ChatMessageSummary, LostReportResponse, PostResponse, InquiryResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| atchFileSn | Long | Y | 이미지 조회 경로는 첨부파일 API로 만든다 | PST_IMG·PRTC_CMPTN_PHOTO·CHAT_MSG_PHOTO·INQRY_ATCH_PHOTO의 ATCH_FILE_SN |
| sortSeq | Integer | Y | | 같은 테이블들의 SORT_SEQ |

### CareMatchCreateRequest
쓰는 API: POST /care-matches
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | 이 채팅방의 게시글 작성자(보호자)만 요청 | PET_PRTC_MTCH.OTO_CHAT_ROOM_SN, CMU-005-06 |
| acrdPrtcBgngDt | LocalDateTime | N | 일정 협의 가능이 아니면 필수(모집 등록 규칙 준용) | MTCH_CND_VER.ACRD_PRTC_BGNG_DT, CMU-005-06 |
| acrdPrtcEndDt | LocalDateTime | N | 시작보다 뒤 | MTCH_CND_VER.ACRD_PRTC_END_DT, CMU-005-06 |
| schdlCnsltnPsbltyYn | Boolean | Y | | MTCH_CND_VER.SCHDL_CNSLTN_PSBLTY_YN |
| acrdPrtcCn | String | Y | 돌봄 내용. 최대 4000자 | MTCH_CND_VER.ACRD_PRTC_CN, CMU-005-06 |
| cstUnitCd | String | Y | 공통코드 PCAG02(시급·건당) | MTCH_CND_VER.CST_UNIT_CD |
| acrdCstAmt | Integer | Y | 원 단위 양의 정수 | MTCH_CND_VER.ACRD_CST_AMT, CMU-005-06 |
| idmptDmndId | String | Y | 중복 요청 방지 | PET_PRTC_MTCH.IDMPT_DMND_ID, CMU-005-11 |

### CareMatchReceiptConfirmRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/confirm-receipt
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rcvYn | Boolean | Y | 실제 수령 여부. false면 돌봄 완료 상태 유지 | PET_PRTC_MTCH.RCV_YN, CMU-005-10 |

### CareMatchResponse
쓰는 API: GET /care-matches/{petPrtcMtchSn}; POST /care-matches
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcMtchSn | Long | Y | | PET_PRTC_MTCH.PET_PRTC_MTCH_SN |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_MTCH.PET_PRTC_RCRT_SN |
| otoChatRoomSn | Long | Y | | PET_PRTC_MTCH.OTO_CHAT_ROOM_SN |
| mtchDlngSttsCd | String | Y | 공통코드 PCAG05 | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD, CMU-005-11 |
| rcvYn | boolean | Y | | PET_PRTC_MTCH.RCV_YN, CMU-005-10 |
| rcvIdntyDt | LocalDateTime | N | | PET_PRTC_MTCH.RCV_IDNTY_DT, CMU-005-11 |
| cnd | CareMatchConditionItem | Y | 현재 조건 버전 | MTCH_CND_VER, CMU-005-09 |
| cmptnDmnds | List<CareCompletionRequestResponse> | Y | 완료 요청 회차 목록(회차 순). 없으면 빈 목록 | PRTC_CMPTN_DMND_CYCL, CMU-005-09 |

#### CareMatchConditionItem (응답 record, 쓰는 DTO: CareMatchResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mtchCndVerSn | Long | Y | | MTCH_CND_VER.MTCH_CND_VER_SN |
| cndVerNo | Integer | Y | | MTCH_CND_VER.CND_VER_NO |
| acrdPrtcBgngDt | LocalDateTime | N | | MTCH_CND_VER.ACRD_PRTC_BGNG_DT |
| acrdPrtcEndDt | LocalDateTime | N | | MTCH_CND_VER.ACRD_PRTC_END_DT |
| schdlCnsltnPsbltyYn | boolean | Y | | MTCH_CND_VER.SCHDL_CNSLTN_PSBLTY_YN |
| acrdPrtcCn | String | Y | | MTCH_CND_VER.ACRD_PRTC_CN |
| cstUnitCd | String | Y | 공통코드 PCAG02 | MTCH_CND_VER.CST_UNIT_CD |
| acrdCstAmt | Integer | Y | 확정 금액(수령 확인 모달 표시) | MTCH_CND_VER.ACRD_CST_AMT, CMU-005-10 |

### CareMatchSupplementRequest
쓰는 API: POST /care-matches/{petPrtcMtchSn}/request-supplement
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| splmntDmndRsn | String | Y | 최대 1000자. 가장 최근 완료 요청 회차에 기록 | PRTC_CMPTN_DMND_CYCL.SPLMNT_DMND_RSN, CMU-005-09 |

### CareRecruitmentCreateRequest
쓰는 API: POST /care-recruitments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pets | List<CareRecruitmentPetRequestItem> | Y | 1개 이상. 강아지·고양이 동시 선택 허용 | RCRT_PET_KND, CMU-004-06 |
| prtcShpCds | List<String> | Y | 1개 이상 복수 선택. 공통코드 PCAG03(방문돌봄·위탁돌봄·산책·놀이·훈련·그 외) | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-06 |
| prtcBgngDt | LocalDateTime | N | 일정 협의 가능이 아니면 필수. 시간은 30분 단위 | PET_PRTC_RCRT.PRTC_BGNG_DT, CMU-004-07 |
| prtcEndDt | LocalDateTime | N | 일정 협의 가능이 아니면 필수. 시작 일시보다 뒤, 30분 단위 | PET_PRTC_RCRT.PRTC_END_DT, CMU-004-07 |
| schdlCnsltnPsbltyYn | Boolean | Y | true면 일정 미정 등록 허용, 입력한 일정은 희망 일정 | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN, CMU-004-07 |
| cstUnitCd | String | Y | 공통코드 PCAG02(시급·건당) | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-08 |
| cstAmt | Integer | Y | 원 단위 양의 정수. 시급이면 최저시급(설정값) 이상. 건당은 비교하지 않는다 | PET_PRTC_RCRT.CST_AMT, SYS_STNG.LOWST_HRWG_AMT, CMU-004-08 |
| rgnSn | Long | Y | | PET_PRTC_RCRT.RGN_SN, CMU-004-10 |
| rlsRgnNm | String | Y | 공개 동네명. 최대 100자 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| prtcPlcDaddr | String | Y | 상세 주소. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_DADDR_ENC, CMU-004-10 |
| prtcPlcLat | BigDecimal | Y | 정확 좌표. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_LAT, CMU-004-10 |
| prtcPlcLot | BigDecimal | Y | 정확 좌표. 공개 응답에 넣지 않는다 | PET_PRTC_RCRT.PRTC_PLC_LOT, CMU-004-10 |
| pstTtl | String | Y | 최대 200자, 공백만 입력 불가 | PST.PST_TTL, CMU-004-09·11 |
| pstCn | String | Y | 최대 4000자, 공백만 입력 불가 | PST.PST_CN, CMU-004-09·11 |
| atchFileSns | List<Long> | N | 최대 10장, 장당 10MB, 확장자 검증 | PST_IMG.ATCH_FILE_SN, CMU-004-09 |
| utztnRulAgreYn | Boolean | Y | 이용수칙·정보 공개 동의. true여야 등록(issues 11) | PET_PRTC_RCRT.UTZTN_RUL_AGRE_YN, CMU-004-11 |

#### CareRecruitmentPetRequestItem (요청 record, 쓰는 DTO: CareRecruitmentCreateRequest, CareRecruitmentUpdateRequest)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01(강아지·고양이) | RCRT_PET_KND.PET_SE_CD, CMU-004-06 |
| vrtySn | Long | N | `vrtySn`·`vrtyDrctInptNm` 중 하나 필수. 해당 구분의 품종 마스터여야 한다 | RCRT_PET_KND.VRTY_SN, CMU-004-13 |
| vrtyDrctInptNm | String | N | '기타' 선택 시 직접 입력. 최대 100자. 마스터에 등록하지 않는다 | RCRT_PET_KND.VRTY_DRCT_INPT_NM, CMU-004-13 |
| petBdwg | BigDecimal | N | 강아지면 필수. 화면은 소형(10kg 미만)·중형(10~25kg 미만)·대형(25kg 이상) 선택(저장 방식 issues 11) | RCRT_PET_KND.PET_BDWG, CMU-004-06 |

### CareRecruitmentResponse
쓰는 API: GET /care-recruitments/{petPrtcRcrtSn}; PATCH /care-recruitments/{petPrtcRcrtSn}; POST /care-recruitments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| pstSn | Long | Y | 좋아요·신고·채팅방 생성에 쓴다 | PET_PRTC_RCRT.PST_SN, CMU-004-12 |
| pstTtl | String | Y | | PST.PST_TTL, CMU-004-04 |
| pstCn | String | Y | | PST.PST_CN, CMU-004-04 |
| mbrSn | Long | Y | 작성자 | PST.MBR_SN, CMU-004-04 |
| mbrNcnm | String | Y | 작성자 | MBR.MBR_NCNM, CMU-004-04 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CMU-004-04 |
| rcrtSttsCd | String | Y | 공통코드 PCAG01(모집중·매칭완료·거래완료) | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-004-04 |
| pets | List<CareRecruitmentPetItem> | Y | | RCRT_PET_KND, CMU-004-04 |
| prtcShpCds | List<String> | Y | 공통코드 PCAG03 | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-04 |
| prtcBgngDt | LocalDateTime | N | | PET_PRTC_RCRT.PRTC_BGNG_DT |
| prtcEndDt | LocalDateTime | N | | PET_PRTC_RCRT.PRTC_END_DT |
| schdlCnsltnPsbltyYn | boolean | Y | | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN |
| cstUnitCd | String | Y | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD |
| cstAmt | Integer | Y | | PET_PRTC_RCRT.CST_AMT |
| rgnSn | Long | Y | | PET_PRTC_RCRT.RGN_SN |
| rlsRgnNm | String | Y | 대략적인 장소. 상세 주소·정확 좌표는 넣지 않는다 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| imgs | List<ImageItem> | Y | 없으면 빈 목록 | PST_IMG, CMU-004-04 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-004-12 |
| liked | Boolean | Y | 내가 좋아요했는지 | PST_RCMDTN.USE_YN, CMU-004-12 |
| owner | Boolean | Y | 작성자 본인인지(수정·삭제 버튼) | PST.MBR_SN, CMU-004-11 |

#### CareRecruitmentPetItem (응답 record, 쓰는 DTO: CareRecruitmentResponse)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01 | RCRT_PET_KND.PET_SE_CD |
| vrtySn | Long | N | | RCRT_PET_KND.VRTY_SN |
| vrtyNm | String | Y | `vrtySn`이 있으면 마스터 품종명, 없으면 직접입력명 | VRTY.VRTY_NM, RCRT_PET_KND.VRTY_DRCT_INPT_NM, CMU-004-13 |
| petBdwg | BigDecimal | N | 강아지 크기 기준 | RCRT_PET_KND.PET_BDWG, CMU-004-06 |

### CareRecruitmentSummary
쓰는 API: GET /care-recruitments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| pstSn | Long | Y | | PET_PRTC_RCRT.PST_SN |
| pstTtl | String | Y | 1행 말줄임 | PST.PST_TTL, CMU-004-01 |
| pstCn | String | Y | 설명 2행 말줄임 | PST.PST_CN, CMU-004-01 |
| thumbnailAtchFileSn | Long | N | 첫 번째 사진. 없으면 null | PST_IMG.ATCH_FILE_SN, CMU-004-01 |
| mbrNcnm | String | Y | 작성자 | MBR.MBR_NCNM, CMU-004-01 |
| rlsRgnNm | String | Y | 지역 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-01 |
| cstUnitCd | String | Y | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-01 |
| cstAmt | Integer | Y | 보수 | PET_PRTC_RCRT.CST_AMT, CMU-004-01 |
| aplcntCnt | Integer | Y | 지원자 수(이 게시글의 채팅방 수) | OTO_CHAT_ROOM.PST_SN, CMU-004-01 |
| rcrtSttsCd | String | Y | 상태 배지. 공통코드 PCAG01 | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-004-01 |

### CareRecruitmentUpdateRequest
쓰는 API: PATCH /care-recruitments/{petPrtcRcrtSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pets | List<CareRecruitmentPetRequestItem> | N | 보내면 1개 이상. 목록 교체 방식(issues 2) | RCRT_PET_KND, CMU-004-06·11 |
| prtcShpCds | List<String> | N | 보내면 1개 이상. 목록 교체 방식 | RCRT_PRTC_SHP.PRTC_SHP_CD, CMU-004-06·11 |
| prtcBgngDt | LocalDateTime | N | 30분 단위 | PET_PRTC_RCRT.PRTC_BGNG_DT, CMU-004-07 |
| prtcEndDt | LocalDateTime | N | 시작 일시보다 뒤, 30분 단위 | PET_PRTC_RCRT.PRTC_END_DT, CMU-004-07 |
| schdlCnsltnPsbltyYn | Boolean | N | | PET_PRTC_RCRT.SCHDL_CNSLTN_PSBLTY_YN, CMU-004-07 |
| cstUnitCd | String | N | 공통코드 PCAG02 | PET_PRTC_RCRT.CST_UNIT_CD, CMU-004-08 |
| cstAmt | Integer | N | 원 단위 양의 정수. 시급이면 최저시급 이상 | PET_PRTC_RCRT.CST_AMT, CMU-004-08 |
| rgnSn | Long | N | | PET_PRTC_RCRT.RGN_SN, CMU-004-10 |
| rlsRgnNm | String | N | 최대 100자 | PET_PRTC_RCRT.RLS_RGN_NM, CMU-004-10 |
| prtcPlcDaddr | String | N | | PET_PRTC_RCRT.PRTC_PLC_DADDR_ENC, CMU-004-10 |
| prtcPlcLat | BigDecimal | N | | PET_PRTC_RCRT.PRTC_PLC_LAT, CMU-004-10 |
| prtcPlcLot | BigDecimal | N | | PET_PRTC_RCRT.PRTC_PLC_LOT, CMU-004-10 |
| pstTtl | String | N | 최대 200자, 공백만 입력 불가 | PST.PST_TTL, CMU-004-09·11 |
| pstCn | String | N | 최대 4000자, 공백만 입력 불가 | PST.PST_CN, CMU-004-09·11 |
| atchFileSns | List<Long> | N | 최대 10장, 장당 10MB. 목록 교체 방식 | PST_IMG.ATCH_FILE_SN, CMU-004-09 |

### ChatMessageCreateRequest
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| msgTypeCd | String | Y | 공통코드 CHTG03(텍스트·사진·주소 공유) | CHAT_MSG.MSG_TYPE_CD, CMU-005-04·05 |
| msgCn | String | N | 텍스트면 필수. 공백만 입력 불가. 길이는 명세 없음 | CHAT_MSG.MSG_CN_ENC, CMU-005-04 |
| shrnAddr | String | N | 주소 공유면 필수. 참여자에게만 공개 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| atchFileSns | List<Long> | N | 사진이면 필수. JPG·PNG·WEBP, 최대 10장, 장당 10MB | CHAT_MSG_PHOTO.ATCH_FILE_SN, CMU-005-05 |
| idmptDmndId | String | Y | 재시도 중복 전송 방지 | CHAT_MSG.IDMPT_DMND_ID, CMU-005-04 |

### ChatMessageResponse
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| chatMsgSn | Long | Y | | CHAT_MSG.CHAT_MSG_SN |
| msgSeq | Integer | Y | 읽음 처리 기준 | CHAT_MSG.MSG_SEQ, CMU-005-03 |
| mbrSn | Long | Y | 보낸 회원 | CHAT_MSG.MBR_SN |
| mine | Boolean | Y | 본인 메시지(오른쪽 배치) | CHAT_MSG.MBR_SN, CMU-005-04 |
| msgTypeCd | String | Y | 공통코드 CHTG03 | CHAT_MSG.MSG_TYPE_CD |
| msgCn | String | N | 복호화 | CHAT_MSG.MSG_CN_ENC |
| shrnAddr | String | N | 복호화. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| photos | List<ImageItem> | Y | 사진 메시지가 아니면 빈 목록 | CHAT_MSG_PHOTO, CMU-005-05 |

### ChatMessageSliceResponse
쓰는 API: GET /chat-rooms/{otoChatRoomSn}/messages (전체 건수를 세지 않는 커서 응답. `PageResponse`를 쓰지 않는다)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| content | List<ChatMessageSummary> | Y | 최신순. 없으면 빈 목록 | CMU-005-04 |
| hasNext | boolean | Y | 더 오래된 메시지가 있는지(`size + 1`건 조회로 판정) | CMU-005-04 |

### ChatMessageSummary
쓰는 API: GET /chat-rooms/{otoChatRoomSn}/messages (ChatMessageSliceResponse.content의 요소)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| chatMsgSn | Long | Y | | CHAT_MSG.CHAT_MSG_SN |
| msgSeq | Integer | Y | 읽음 처리 기준 | CHAT_MSG.MSG_SEQ, CMU-005-03 |
| mbrSn | Long | Y | 보낸 회원 | CHAT_MSG.MBR_SN |
| mine | Boolean | Y | 본인 메시지(오른쪽 배치) | CHAT_MSG.MBR_SN, CMU-005-04 |
| msgTypeCd | String | Y | 공통코드 CHTG03 | CHAT_MSG.MSG_TYPE_CD |
| msgCn | String | N | 복호화 | CHAT_MSG.MSG_CN_ENC, CMU-005-04 |
| shrnAddr | String | N | 복호화. 참여자에게만 | CHAT_MSG.SHRN_ADDR_ENC, CMU-005-05 |
| photos | List<ImageItem> | Y | 사진 메시지가 아니면 빈 목록 | CHAT_MSG_PHOTO, CMU-005-05 |

### ChatRoomCreateRequest
쓰는 API: POST /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | 타인의 모집중 펫돌봄 게시글. 게시글·보호자·지원자 조합에 방이 있으면 기존 방을 돌려준다 | OTO_CHAT_ROOM.PST_SN, CMU-004-05, CMU-005-01 |

### ChatRoomReadRequest
쓰는 API: POST /chat-rooms/{otoChatRoomSn}/read
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lastPrslMsgSeq | Integer | Y | 활성 대화창에 표시된 마지막 메시지 순서. 저장값보다 작으면 무시 | CHAT_PRTPNT.LAST_PRSL_MSG_SEQ, CMU-005-03 |

### ChatRoomResponse
쓰는 API: GET /chat-rooms/{otoChatRoomSn}; POST /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | | OTO_CHAT_ROOM.OTO_CHAT_ROOM_SN |
| chatRoomSttsCd | String | Y | 공통코드 CHTG01 | OTO_CHAT_ROOM.CHAT_ROOM_STTS_CD |
| pstSn | Long | Y | 상단 게시글 카드 | OTO_CHAT_ROOM.PST_SN, CMU-005-01 |
| pstTtl | String | Y | 상단 게시글 카드 | PST.PST_TTL, CMU-005-01 |
| petPrtcRcrtSn | Long | Y | | PET_PRTC_RCRT.PET_PRTC_RCRT_SN |
| rcrtSttsCd | String | Y | 공통코드 PCAG01 | PET_PRTC_RCRT.RCRT_STTS_CD, CMU-005-07 |
| ctptMbrSn | Long | Y | 상대 회원 | OTO_CHAT_ROOM.CTPT_MBR_SN |
| ctptMbrNcnm | String | Y | | MBR.MBR_NCNM, CMU-005-04 |
| ctptProfileAtchFileSn | Long | N | 상대 메시지 왼쪽 프로필 | MBR.ATCH_FILE_SN, CMU-005-04 |
| myRole | String | Y | 이 방에서 내 역할(보호자/제공자, 값 미정). 실행 버튼 결정 | CMU-005-11 |
| petPrtcMtchSn | Long | N | 이 방의 매칭. 요청 전이면 null | PET_PRTC_MTCH.PET_PRTC_MTCH_SN, CMU-005-11 |
| mtchDlngSttsCd | String | N | 매칭 진행 상태. 공통코드 PCAG05 | PET_PRTC_MTCH.MTCH_DLNG_STTS_CD, CMU-005-11 |

### ChatRoomSummary
쓰는 API: GET /chat-rooms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoChatRoomSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.OTO_CHAT_ROOM_SN |
| chatRoomSttsCd | String | Y | 초안 그대로. 공통코드 CHTG01 | OTO_CHAT_ROOM.CHAT_ROOM_STTS_CD |
| pstSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.PST_SN |
| pstTtl | String | Y | 초안 그대로. 1행 말줄임 | PST.PST_TTL |
| ctptMbrSn | Long | Y | 초안 그대로 | OTO_CHAT_ROOM.CTPT_MBR_SN |
| ctptMbrNcnm | String | Y | 초안 그대로 | MBR.MBR_NCNM |
| ctptProfileAtchFileSn | Long | N | 초안 그대로 | MBR.ATCH_FILE_SN |
| lastMsgCn | String | N | 초안 그대로. 1행 말줄임 | CHAT_MSG.MSG_CN_ENC (복호화) |
| lastMsgDt | LocalDateTime | N | 초안 그대로 | CMU-005-02 |
| unreadCnt | Integer | Y | 초안 그대로 | CHAT_PRTPNT.LAST_PRSL_MSG_SEQ, CHAT_MSG.MSG_SEQ |

### ChatUnreadCountResponse
쓰는 API: GET /chat-rooms/unread-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| unreadCnt | Integer | Y | 초안 그대로. 실제 수를 주고 99+ 표시는 화면이 한다 | CMU-005-03 |

### CommentCreateRequest
쓰는 API: POST /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntCn | String | Y | 최대 500자, 공백만이면 400 | CMNT.CMNT_CN, CMU-001-11, CMU-002-07 |
| prntCmntSn | Long | N | 답글이면 대상 댓글. 대댓글을 고르면 그 대댓글의 최상위 댓글에 붙는다(1depth 유지, 화면설계서 "원 댓글 스레드에 이어서"). 다른 글의 댓글이거나 삭제된 댓글이면 404 | CMNT.PRNT_CMNT_SN, CMU-001-11 |

### CommentResponse
쓰는 API: POST /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntSn | Long | Y | | CMNT.CMNT_SN |
| pstSn | Long | Y | | CMNT.PST_SN |
| prntCmntSn | Long | N | | CMNT.PRNT_CMNT_SN |
| cmntCn | String | Y | | CMNT.CMNT_CN |
| mbrSn | Long | Y | | CMNT.MBR_SN |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| regDt | LocalDateTime | Y | 화면의 "1시간 전" 표시용 | CMNT.REG_DT, CMU-001-11 |

### CommentSummary
쓰는 API: GET /posts/{pstSn}/comments
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntSn | Long | Y | | CMNT.CMNT_SN |
| prntCmntSn | Long | N | 대댓글이면 부모 댓글 | CMNT.PRNT_CMNT_SN, CMU-001-11 |
| delYn | boolean | Y | true면 화면이 "삭제된 댓글입니다"로 표시(issues 14) | CMNT.DEL_YN, CMU-001-11 |
| cmntCn | String | N | 삭제된 댓글이면 null | CMNT.CMNT_CN |
| mbrSn | Long | N | 삭제된 댓글이면 null | CMNT.MBR_SN |
| mbrNcnm | String | N | 삭제된 댓글이면 null | MBR.MBR_NCNM |
| sysAutoCrtYn | boolean | Y | 찾았어요 완료 안내 자동 댓글 | CMNT.SYS_AUTO_CRT_YN, CMU-002-09 |
| owner | Boolean | Y | 내 댓글인지(삭제 버튼) | CMNT.MBR_SN, CMU-001-11 |
| regDt | LocalDateTime | Y | 화면의 "1시간 전" 표시용 | CMNT.REG_DT, CMU-001-11 |

### CommunityAiCommentaryResponse
쓰는 API: GET /community-ai-commentaries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmntyAiCmtrySn | Long | N | 해당 일자 코멘트가 없어 기본 문구를 주면 null | CMNTY_AI_CMTRY.CMNTY_AI_CMTRY_SN |
| cmtryCrtrYmd | LocalDate | Y | | CMNTY_AI_CMTRY.CMTRY_CRTR_YMD, CMU-001-05 |
| cmtryCn | String | Y | 없으면 기본 내용 | CMNTY_AI_CMTRY.CMTRY_CN, SYS_STNG.AI_CMTRY_BSC_CN, CMU-001-05 |

### LostReportCreateRequest
쓰는 API: POST /lost-reports
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | 공통코드 PETG01 | LOST_TPOF.PET_SE_CD, CMU-002-10 |
| petNm | String | Y | 최대 100자 | LOST_TPOF.PET_NM, CMU-002-10 |
| vrtySn | Long | N | `vrtySn`·`vrtyDrctInptNm` 중 하나 필수. 해당 구분의 품종 마스터여야 한다 | LOST_TPOF.VRTY_SN, CMU-002-13 |
| vrtyDrctInptNm | String | N | '기타' 선택 시 직접 입력. 최대 100자. 마스터에 등록하지 않는다 | LOST_TPOF.VRTY_DRCT_INPT_NM, CMU-002-13 |
| colrNm | String | Y | 털 색상. 최대 100자 | LOST_TPOF.COLR_NM, CMU-002-10 |
| chrcCn | String | N | 특징. 최대 40자 | LOST_TPOF.CHRC_CN, CMU-002-10 |
| lastWtnsDt | LocalDateTime | Y | 마지막 목격 날짜(화면은 날짜, 컬럼은 일시, issues 12) | LOST_TPOF.LAST_WTNS_DT, CMU-002-10 |
| rgnSn | Long | Y | 실종지역 | LOST_TPOF.RGN_SN, CMU-002-10 |
| wtnsPlcNm | String | N | 목격 장소명. 최대 200자 | LOST_TPOF.WTNS_PLC_NM, CMU-002-10 |
| wtnsPlcLat | BigDecimal | Y | 지도 핀 좌표 | LOST_TPOF.WTNS_PLC_LAT, CMU-002-11 |
| wtnsPlcLot | BigDecimal | Y | 지도 핀 좌표 | LOST_TPOF.WTNS_PLC_LOT, CMU-002-11 |
| cnpl | String | Y | 전화번호. 형식은 명세 없음 | LOST_TPOF.CNPL_ENC, CMU-002-10 |
| pstTtl | String | Y | 최대 200자. 펫이야기는 50자로 바뀌었으니 SCR-CMU-002 기획에서 명세로 확정 | PST.PST_TTL, CMU-002-10, CMU-001-14 |
| pstCn | String | Y | 최대 4000자. 펫이야기는 2000자로 바뀌었으니 SCR-CMU-002 기획에서 명세로 확정 | PST.PST_CN, CMU-002-10, CMU-001-14 |
| atchFileSns | List<Long> | N | 최대 10장, 장당 10MB, 확장자 검증(펫이야기와 동일) | PST_IMG.ATCH_FILE_SN, CMU-002-10 |
| prvcRlsAgreYn | Boolean | Y | 개인정보 수집·이용 및 게시글 공개 동의. true여야 등록 | LOST_TPOF.PRVC_RLS_AGRE_YN, CMU-002-10 |

### LostReportResponse
쓰는 API: GET /lost-reports/{lostTpofSn}; POST /lost-reports
찾았어요 상태면 작성자가 아닌 회원에게 제목·상태·처리 시각만 주고 나머지 N 필드는 null이다(CMU-002-09).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | | LOST_TPOF.LOST_TPOF_SN |
| pstSn | Long | Y | 좋아요·댓글·신고에 쓴다 | LOST_TPOF.PST_SN |
| pstTtl | String | Y | | PST.PST_TTL, CMU-002-09 |
| lostTpofSttsCd | String | Y | 공통코드 CMUG03(실종·찾았어요) | LOST_TPOF.LOST_TPOF_STTS_CD, CMU-002-09 |
| dscvSwtcDt | LocalDateTime | N | 찾았어요 전환 시각 | LOST_TPOF.DSCV_SWTC_DT, CMU-002-09 |
| pstCn | String | N | | PST.PST_CN, CMU-002-05 |
| petSeCd | String | N | 공통코드 PETG01 | LOST_TPOF.PET_SE_CD, CMU-002-05 |
| petNm | String | N | | LOST_TPOF.PET_NM |
| vrtySn | Long | N | | LOST_TPOF.VRTY_SN |
| vrtyNm | String | N | `vrtySn`이 있으면 마스터 품종명, 없으면 직접입력명 | VRTY.VRTY_NM, LOST_TPOF.VRTY_DRCT_INPT_NM, CMU-002-13 |
| colrNm | String | N | | LOST_TPOF.COLR_NM, CMU-002-05 |
| chrcCn | String | N | | LOST_TPOF.CHRC_CN |
| lastWtnsDt | LocalDateTime | N | | LOST_TPOF.LAST_WTNS_DT, CMU-002-05 |
| rgnSn | Long | N | | LOST_TPOF.RGN_SN |
| rgnNm | String | N | 시도·시군구·읍면동 조합 | RGN.CTPV_NM, RGN.SGG_NM, RGN.EMD_NM |
| wtnsPlcNm | String | N | | LOST_TPOF.WTNS_PLC_NM, CMU-002-05 |
| wtnsPlcLat | BigDecimal | N | 지도 표시 | LOST_TPOF.WTNS_PLC_LAT, CMU-002-06 |
| wtnsPlcLot | BigDecimal | N | 지도 표시 | LOST_TPOF.WTNS_PLC_LOT, CMU-002-06 |
| cnpl | String | N | 작성자 입력값 그대로(복호화). 로그인 회원만 | LOST_TPOF.CNPL_ENC, CMU-002-05 |
| expryCrtrDt | LocalDateTime | N | 실종 상태 만료 기준 일시(갱신 버튼) | LOST_TPOF.EXPRY_CRTR_DT, CMU-002-09 |
| imgs | List<ImageItem> | N | | PST_IMG, CMU-002-05 |
| inqCnt | Integer | N | | PST.INQ_CNT, CMU-002-07 |
| rcmdtnCnt | Integer | N | | PST.RCMDTN_CNT, CMU-002-08 |
| liked | Boolean | Y | | PST_RCMDTN.USE_YN, CMU-002-08 |
| owner | Boolean | Y | 작성자 본인인지(찾았어요·갱신 버튼) | PST.MBR_SN, CMU-002-09 |

### LostReportSummary
쓰는 API: GET /lost-reports
찾았어요 상태 카드는 작성자가 아니면 N 필드가 null이다(CMU-002-09).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| lostTpofSn | Long | Y | | LOST_TPOF.LOST_TPOF_SN |
| pstTtl | String | Y | | PST.PST_TTL, CMU-002-09 |
| lostTpofSttsCd | String | Y | 배지·톤다운 구분. 공통코드 CMUG03 | LOST_TPOF.LOST_TPOF_STTS_CD, CMU-002-02 |
| dscvSwtcDt | LocalDateTime | N | | LOST_TPOF.DSCV_SWTC_DT, CMU-002-09 |
| petNm | String | N | 이름 | LOST_TPOF.PET_NM, CMU-002-01 |
| rgnNm | String | N | 지역. 시도·시군구·읍면동 조합 | RGN.CTPV_NM, RGN.SGG_NM, RGN.EMD_NM, CMU-002-01 |
| colrNm | String | N | 털색상 | LOST_TPOF.COLR_NM, CMU-002-01 |
| chrcCn | String | N | 특징 | LOST_TPOF.CHRC_CN, CMU-002-01 |

### NewsResponse
쓰는 API: GET /news/{newsSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD |
| newsSrcCd | String | Y | 공통코드 CMUG07(외부 언론사·관리자 등록) | NEWS.NEWS_SRC_CD, CMU-003-04 |
| newsSrcCdNm | String | Y | 출처 표시 | COM_CD.COM_CD_NM, CMU-003-04 |
| srcPressNm | String | N | 외부 기사 언론사 | NEWS.SRC_PRESS_NM, CMU-003-04 |
| newsTtl | String | Y | | NEWS.NEWS_TTL |
| newsCn | String | Y | 외부 기사는 요약만 | NEWS.NEWS_CN, CMU-003-04·06 |
| orgtxtPath | String | N | 원문보기 링크 | NEWS.ORGTXT_PATH, CMU-003-04 |
| pblcnDt | LocalDateTime | Y | 작성일 | NEWS.PBLCN_DT, CMU-003-04 |
| thmbUrlPath | String | N | 외부 기사 썸네일 URL | NEWS.THMB_URL_PATH, CMU-003-06 |
| atchFileSn | Long | N | 관리자 등록 뉴스 이미지 | NEWS.ATCH_FILE_SN |

### NewsSummary
쓰는 API: GET /news
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| newsSn | Long | Y | | NEWS.NEWS_SN |
| newsClsfCd | String | Y | 공통코드 CMUG06 | NEWS.NEWS_CLSF_CD, CMU-003-02 |
| newsSrcCd | String | Y | 공통코드 CMUG07 | NEWS.NEWS_SRC_CD, CMU-003-01 |
| newsSrcCdNm | String | Y | 출처 배지 | COM_CD.COM_CD_NM, CMU-003-01 |
| newsTtl | String | Y | | NEWS.NEWS_TTL, CMU-003-01 |
| pblcnDt | LocalDateTime | Y | 최신순 기준 | NEWS.PBLCN_DT, CMU-003-01 |
| thmbUrlPath | String | N | | NEWS.THMB_URL_PATH, CMU-003-06 |
| atchFileSn | Long | N | | NEWS.ATCH_FILE_SN |

### PostCreateRequest
쓰는 API: POST /posts
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bbsCd | String | Y | 공통코드 CMUG01(정보·일상) | PST.BBS_CD, CMU-001-14 |
| pstTtl | String | Y | 최대 50자 | PST.PST_TTL, CMU-001-14 |
| pstCn | String | Y | 최대 2000자 | PST.PST_CN, CMU-001-14 |
| images | List<MultipartFile> | N | multipart/form-data. 최대 10장, 장당 10MB, jpg·jpeg·png·webp. 첫 장이 대표 사진. 공용 업로드 API가 없어 파일을 같이 받는다 | PST_IMG.ATCH_FILE_SN, CMU-001-14 |

### PostLikeResponse
쓰는 API: POST /posts/{pstSn}/likes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST_RCMDTN.PST_SN |
| rcmdtnCnt | Integer | Y | 반영 후 좋아요 수 | PST.RCMDTN_CNT, CMU-001-09 |

### PostResponse
쓰는 API: GET /posts/popular-photo; GET /posts/{pstSn}; PATCH /posts/{pstSn}; POST /posts
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN |
| bbsCd | String | Y | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-10 |
| pstTtl | String | Y | | PST.PST_TTL, CMU-001-10 |
| pstCn | String | Y | | PST.PST_CN, CMU-001-10 |
| mbrSn | Long | Y | 작성자 | PST.MBR_SN, CMU-001-10 |
| mbrNcnm | String | Y | 작성자 | MBR.MBR_NCNM, CMU-001-10 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CMU-001-10 |
| inqCnt | Integer | Y | | PST.INQ_CNT, CMU-001-10 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-001-09 |
| cmntCnt | Integer | Y | 삭제 댓글 제외 | CMNT.DEL_YN, CMU-001-11 |
| imgs | List<ImageItem> | Y | 없으면 빈 목록. 인기 포토는 첫 장 표시 | PST_IMG, CMU-001-06·10 |
| liked | Boolean | Y | 내가 좋아요했는지 | PST_RCMDTN.USE_YN, CMU-001-09 |
| owner | Boolean | Y | 작성자 본인인지(수정·삭제) | PST.MBR_SN, CMU-001-15 |

### PostSummary
쓰는 API: GET /posts; GET /posts/health-tips
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | | PST.PST_SN |
| bbsCd | String | Y | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-02 |
| pstTtl | String | Y | 1행 말줄임 | PST.PST_TTL, CMU-001-01 |
| pstCn | String | Y | 2행 말줄임 | PST.PST_CN, CMU-001-01 |
| mbrNcnm | String | Y | 작성자 | MBR.MBR_NCNM, CMU-001-01 |
| regDt | LocalDateTime | Y | 작성일("1시간 전" 표시용) | 공통 감사 컬럼 REG_DT, CMU-001-01 |
| thumbAtchFileSn | Long | N | 대표 사진(첫 장). 사진이 없으면 null | PST_IMG.ATCH_FILE_SN, CMU-001-01 |
| rcmdtnCnt | Integer | Y | | PST.RCMDTN_CNT, CMU-001-03 |
| cmntCnt | Integer | Y | 삭제 댓글 제외 | CMNT.DEL_YN, CMU-001-11 |
| popular | Boolean | Y | 인기글 배지(최근 7일, 좋아요 1개 이상 상위 5개). 인기순 정렬이면 false | CMU-001-03·04 |

### PostUpdateRequest
쓰는 API: PATCH /posts/{pstSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bbsCd | String | N | 공통코드 CMUG01 | PST.BBS_CD, CMU-001-15 |
| pstTtl | String | N | 최대 50자, 공백만이면 400 | PST.PST_TTL, CMU-001-14·15 |
| pstCn | String | N | 최대 2000자, 공백만이면 400 | PST.PST_CN, CMU-001-14·15 |
| keepAtchFileSns | List<Long> | N | 남길 기존 사진. 보내면 목록에 없는 기존 사진은 삭제(DEL_YN='Y'), 안 보내면 모두 유지 | PST_IMG.ATCH_FILE_SN, CMU-001-15 |
| images | List<MultipartFile> | N | multipart/form-data. 새로 추가할 사진. 남은 사진과 합쳐 최대 10장 | PST_IMG.ATCH_FILE_SN, CMU-001-15 |

### ReportCreateRequest
쓰는 API: POST /reports
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | N | `pstSn`·`cmntSn` 중 하나만 필수 | DCLR.PST_SN, CMU-001-13 |
| cmntSn | Long | N | 같은 회원의 같은 댓글 중복 신고 불가 | DCLR.CMNT_SN, CMU-001-13 |
| dclrRsnCd | String | Y | 공통코드 CMUG04(스팸·광고성/욕설·비방/음란물·유해콘텐츠/개인정보 노출/사기·허위 정보/기타) | DCLR.DCLR_RSN_CD, CMU-001-13 |
| dclrDtlCn | String | N | 최대 500자. 사유가 기타(CMUC0014)면 필수 | DCLR.DCLR_DTL_CN, CMU-001-13 |

### ReportResponse
쓰는 API: POST /reports
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dclrSn | Long | Y | | DCLR.DCLR_SN |
| dclrPrcsSttsCd | String | Y | 공통코드 CMUG05 | DCLR.DCLR_PRCS_STTS_CD, CMU-004-12 |
