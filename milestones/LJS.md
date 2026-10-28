# LJS 마일스톤

기준: 공통레이아웃_기능명세서_v0.4_20261001, AI매니저_기능명세서_v0.7_20261009, 마이페이지_기능명세서_v0.4_20261019, 건강기록_기능명세서_v1.0_20261018 (DMS, 2026-10-20 확인)

## SCR-COM-002 기본 레이아웃 — 끝
- 기능: FN-COM-002-02 로그인 필수 접근 제어, FN-COM-002-15 프로필 아이콘(회원 요약), FN-COM-002-24 세션 만료 처리, FN-COM-003-01 미인증 접근 감지(화면은 SCR-COM-003), FN-COM-003-07 세션 만료 동일 처리(화면은 SCR-COM-003), FN-COM-005-02 회원 정보 표시(화면은 SCR-COM-005)
- 함께 만드는 것: `global/response`(ApiResponse·PageResponse·PageQuery), `global/error`(ErrorCode·BusinessException·GlobalExceptionHandler), `global/security`(LoginMember·JWT 발급·검증 필터·httpOnly 쿠키·SecurityConfig·401/403 응답·X-Requested-With 검사·개발용 토큰 발급), `global/security/crypto`(FieldCryptoService·TokenHashUtil), `global/log`(요청 로그·MDC·민감정보 마스킹), CORS 설정, CI 워크플로, 응답 모양을 nct-backend 형식으로 바꾼 규칙 문서, 테스트용 `@WithMockLoginMember`
- 기다리는 것: 없음. 로그인·로그아웃·재발급 API(YAY)는 이 토큰 기반 위에 만든다. 그 전까지 개발용 토큰 발급 API로 쿠키를 받는다
- 완료 조건: 쿠키 없이 `/api/v1/**`를 부르면 401 `UNAUTHORIZED`, 만료 토큰이면 401 `TOKEN_EXPIRED`가 nct 응답 모양으로 나온다. `GET /api/v1/auth/me`가 회원 번호·권한·닉네임·첨부파일 번호·마스킹 이메일을 돌려준다
- 연결 남음: 개발용 토큰 발급 (로그인·로그아웃·재발급 API가 생기면 DevTokenController 삭제, YAY)
- 연결 남음: FN-COM-002-15 (프로필 이미지 URL, 파일 조회 API SCR-MYP-010, LJS)

## SCR-ACC-002 로그인 준비 (토큰 세션 기반, 로그인 API는 YAY) — 끝
- 기능: 없음(화면 기능은 YAY). SCR-ACC-002 로그인·재발급·로그아웃이 쓸 `global/security` 기반만 만든다
- 함께 만드는 것: JWT 라이브러리 Nimbus 교체, 토큰 `sid`(RFRSH_TKN_SN)·`typ`, `LoginSessionManager`(세션 발급·회전·폐기), 필터의 세션 확인, `PasswordEncoder`, DevTokenController를 새 발급 방식으로
- 기다리는 것: 없음
- 완료 조건: 발급한 세션을 폐기하면 그 Access로 `/api/auth/me`가 401. 같은 Refresh로 두 번 회전하면 두 번째는 `UNAUTHORIZED`
- 추가 요청: 추가-1 SecurityConfigTest sid 시그니처, 추가-2 스타터 방식 화이트리스트(`app.security.permit-all-paths`)

## SCR-COM-004 알림 드롭다운 — 끝
- 기능: FN-COM-002-09 알림 아이콘·미확인 배지(화면은 SCR-COM-002), FN-COM-004-02 미읽음 개수 표시, FN-COM-004-03 유형 탭 노출, FN-COM-004-04 유형 탭 필터, FN-COM-004-05 알림 목록 조회, FN-COM-004-06 알림 클릭·읽음 처리, FN-COM-004-07 유형별 이동 대상 분기, FN-COM-004-08 공동양육 초대 즉시 처리, FN-COM-004-09 펫돌봄 거래 상태 알림 처리, FN-COM-004-10 캐릭터 완료 알림 처리, FN-COM-004-11 자동 읽음 미처리, FN-COM-004-12 7일 경과 자동 숨김, FN-COM-004-13 수신 OFF 유형 미생성, FN-COM-008-16 수신 OFF = 미생성(화면은 SCR-COM-008), FN-AIM-006-06 유형 수신 설정 확인(화면은 SCR-AIM-006), FN-AIM-006-12 헤더 뱃지 갱신(화면은 SCR-AIM-006)
- API가 남의 것: FN-COM-004-08 초대 수락·거절 (SJM)
- 함께 만드는 것: `global/notification`(NotificationEvent·MailEvent·EventPublisher), `notification` 도메인의 알림 저장 리스너
- 기다리는 것: 없음 (초대 수락·거절 SJM은 가짜 응답으로 먼저)
- 완료 조건: `EventPublisher.publish(NotificationEvent)`가 트랜잭션 안에서는 INAPP_NOTI에 저장되고 밖에서는 예외를 던진다. 미읽음 개수와 최근 5건 목록 API가 7일 지난 알림을 빼고 돌려준다
- 연결 남음: FN-COM-004-08 (공동양육 초대 수락·거절 API와 초대 알림 mvmnPath 형식, SJM)

## SCR-MYP-010 픽셀 캐릭터 관리 — 끝
- 기능: FN-MYP-001-01 내 반려동물 카드의 캐릭터 얼굴(화면은 SCR-MYP-001), FN-MYP-002-02 반려동물 슬라이드의 캐릭터 얼굴(화면은 SCR-MYP-002), FN-MYP-010-01 탭 구성, FN-MYP-010-02 내 캐릭터 목록, FN-MYP-010-03 캐릭터 만들기, FN-MYP-010-04 생성 상한, FN-MYP-010-07 공동양육 탭, FN-MYP-010-08 1단계 사진 첨부, FN-MYP-010-09 2단계 생성 중, FN-MYP-010-10 3단계 결과 확인, FN-MYP-010-12 생성 실패 시 대체, FN-MYP-010-13 미리보기·부위 슬롯, FN-MYP-010-14 보관함·카테고리 탭, FN-MYP-010-16 되돌리기·장착, FN-MYP-010-17 캐릭터 규격 고정, FN-MYP-010-18 규격 일관성 검증, FN-MYP-010-19 규격 미확보 시 대체, FN-MYP-010-23 생성 오류 처리, FN-MYP-010-24 오류 문의
- API가 남의 것: FN-MYP-010-24 1:1 문의 작성 (KSJ), FN-MYP-010-23 관리자 확인 화면 (관리자 화면 미정)
- 함께 만드는 것: `global/file`(FileStore·파일 조회 API), `global/auth`(PetAuthorizer·OwnerAuthorizer), `global/setting`(SYS_STNG 조회, FileStore의 파일 최대 크기 FILE_MAX_SZ), `global/code`(공통코드 조회, 부위 탭 MYPG01 FN-MYP-010-14), `global/external`(캐릭터 생성 API 호출)
- 기다리는 것: pet 목록·프로필 조회 (SJM), 캐릭터 생성 외부 API 결정
- 완료 조건: 캐릭터가 없는 반려동물은 종별 기본 캐릭터 얼굴 이미지를 돌려준다. 생성 요청은 반려동물당 1회만 받고 완료 시 캐릭터 알림을 발행한다
- 연결 남음: FN-MYP-010-24 (1:1 문의 작성 화면에서 receiveErrorInquiry 호출, KSJ)
- 연결 남음: FN-MYP-010-23 (관리자 확인 화면에서 finishErrorCheck 호출, 담당 미정)
- 다음으로 넘김: FN-MYP-010-19 (캐릭터 생성 외부 API 결정 뒤, LJS)

## SCR-COM-008 설정 (알림) — 끝
- 기능: FN-COM-008-02 알림 채널 설정, FN-COM-008-03 이메일 주소 고정 표시, FN-COM-008-04 양 채널 OFF 처리, FN-COM-008-05 케어 일정 수신 설정, FN-COM-008-06 사전 알림 시점 설정, FN-COM-008-07 펫돌봄 거래 상태 수신 설정, FN-COM-008-08 커뮤니티 수신 설정, FN-COM-008-09 가계부 알림 수신 설정, FN-COM-008-10 한도 미설정 시 안내, FN-COM-008-11 항상 받는 알림 표시, FN-COM-008-12 캐릭터 완료 알림 배치, FN-COM-008-13 기본값 적용, FN-COM-008-14 저장 처리, FN-COM-008-20 계정 단위 적용
- API가 남의 것: FN-COM-008-10 월 지출 한도 조회 (SJM) → MBR.MM_EXPND_LIM_AMT를 직접 읽어 연결 불필요
- 기다리는 것: 없음
- 완료 조건: 설정 조회가 MBR의 수신 여부 컬럼을 true/false로 돌려준다. 저장(PUT)이 항상 받는 알림은 바꾸지 않는다

## SCR-MYP-011 아이템 상점 모달 — 끝
- 기능: FN-MYP-011-02 카테고리 탭, FN-MYP-011-03 아이템 목록·보유중 표시, FN-MYP-011-05 구매, FN-MYP-011-06 권한
- 기다리는 것: pet 포인트 차감 (SJM)
- 완료 조건: 이미 보유한 아이템은 보유중으로 표시된다. 포인트가 부족하면 `POINT_NOT_ENOUGH`로 구매를 막는다
- 연결 남음: FN-MYP-011-05 (pet 포인트 조회·차감 FN-MYP-020, SJM)

## SCR-AIM-006 케어 일정 사전 알림 — 끝
- 기능: FN-AIM-006-01 발송 대상 추출, FN-AIM-006-02 알림 대상 범위, FN-AIM-006-03 발송 시점 산출, FN-AIM-006-04 배치 스캔 실행, FN-AIM-006-05 채널 판정, FN-AIM-006-07 발송 주소 결정, FN-AIM-006-09 템플릿 치환, FN-AIM-006-10 이메일 발송, FN-AIM-006-11 인앱 알림 생성, FN-AIM-006-13 알림 클릭 이동, FN-AIM-006-14 수신 해제 안내, FN-AIM-006-15 발송 이력 기록, FN-AIM-006-16 실패 재시도, FN-AIM-006-17 중복 발송 방지, FN-AIM-006-18 스캔 시점 판정, FN-AIM-006-19 스캔 이후 등록분 처리, FN-AIM-006-20 수신자 결정, FN-COM-008-19 수신 해제 링크 도착(화면은 SCR-COM-008)
- 함께 만드는 것: `global/mail` 메일 발송, `global/batch`(스케줄러 공통 설정, 배치 스캔 FN-AIM-006-04)
- 기다리는 것: health 일정 조회 (JAJ), pet 공동양육자 조회 (SJM). 배치 위치(`notification`·`scheduler`)는 docs/OPEN_ISSUES.md에서 정한다
- 완료 조건: 09:00 스캔이 D-N이 맞는 병원 일정만 골라 수신자별로 한 번만 보낸다. 발송 이력이 NOTI_SNDNG_LOG에 남는다
- 연결 남음: FN-AIM-006-01·02 (health 병원 일정 조회, JAJ), FN-AIM-006-20 (pet 연동 양육자 조회, SJM), FN-AIM-006-11·13 화면 확인 (위 두 연결 뒤, LJS), FN-AIM-006-10 실제 SMTP 발송 (팀 SMTP 계정, 팀)

## SCR-AIM-008 영수증 촬영·업로드 (1차: 업로드·OCR·영수증 상태) — 끝
- 기능: FN-AIM-008-02 역할 분리, FN-AIM-008-03 대상 반려동물 선택, FN-AIM-008-04 촬영·파일 업로드, FN-AIM-008-05 업로드 제약 검증, FN-AIM-008-06 OCR API 호출, FN-AIM-008-07 분석 대기 표시, FN-AIM-008-09 원본 이미지 보관, FN-AIM-009-04 OCR 값 → 필드 변환, FN-AIM-009-16 반려동물 탭 구성, FN-AIM-009-17 반려동물 자동 연결·선택(결과 응답 쪽), FN-AIM-009-20 하단 할인 비율 배분, FN-AIM-009-21 검진 내용 자동 채움, FN-AIM-010-01 인식 실패·일시 오류 판정, FN-AIM-010-05 부분 인식 처리, FN-AIM-010-06 원본 이미지 유지, FN-AIM-010-07 실패 사유 구분 안내
- API가 남의 것: 연동 반려동물 목록 (SJM, 가짜 `CaregiverPetFake`)
- 함께 만드는 것: `global/external`(OCR 호출 FN-AIM-008-06, 가짜 `FakeOcrClient`), `global/file/FileStore` 실제 파일 형식 검사
- 기다리는 것: OCR 외부 API 규격
- 위치: `expense` 도메인(`docs/OPEN_ISSUES.md`, 2026-10-21)
- 완료 조건: 업로드가 jpg·jpeg·png·webp 10MB 이하만 받고(실제 형식 검사) RECPT에 원본을 남긴다. 분석 결과가 인식성공·부분인식·인식실패·일시오류(사유 2종)·취소로 RECPT 상태와 RECPT_STTS_HSTRY에 남는다
- 연결 남음: FN-AIM-009-17 (연동 반려동물 목록, SJM — 지금은 `CaregiverPetFake` 빈 목록)
- 연결 남음: FN-AIM-008-06 (OCR 외부 API 규격, 담당 미정 — 지금은 `FakeOcrClient`)

## SCR-AIM-008 영수증 저장 (2차: 확인 후 저장) — 끝
- 기능: FN-AIM-009-01 원본 이미지 대조 표시, FN-AIM-009-02 공통 항목 표시·수정, FN-AIM-009-03 처방 약품 다건 행 표시, FN-AIM-009-07 필수값 검증, FN-AIM-009-08 확인 후 저장, FN-AIM-009-09 검진기록 생성·연동, FN-AIM-009-10 가계부 기록, FN-AIM-009-11 투약 항목 자동 추가 미적용, FN-AIM-009-12 저장 후 이동, FN-AIM-009-13 금액 불일치 안내, FN-AIM-009-18 반려동물 추가, FN-AIM-009-19 배정 필요 목록, FN-AIM-009-22 가계부에도 기록 선택, FN-AIM-009-23 가계부 중복 확인, FN-AIM-009-24 일괄 저장, FN-AIM-009-26 입력 길이 제한, FN-AIM-009-27 영수증 상태 갱신, FN-AIM-010-08 인식 실패율 수집(명세서 "이번 차수 제외"), FN-HLT-002-34 원본 이미지(화면은 SCR-HLT-002), FN-HLT-002-35 검진일·병원명(화면은 SCR-HLT-002), FN-HLT-002-36 반려동물별 기록 탭(화면은 SCR-HLT-002), FN-HLT-002-37 검진 종류(화면은 SCR-HLT-002), FN-HLT-002-38 검진 내용·검진 메모(화면은 SCR-HLT-002), FN-HLT-002-39 금액(화면은 SCR-HLT-002), FN-HLT-002-40 처방 약품(화면은 SCR-HLT-002), FN-HLT-002-41 가계부에도 기록(화면은 SCR-HLT-002), FN-HLT-002-42 영수증 금액·할인 배분(화면은 SCR-HLT-002), FN-HLT-002-43 배정 필요 목록(화면은 SCR-HLT-002), FN-HLT-002-44 가계부 중복 확인(화면은 SCR-HLT-002), FN-HLT-002-45 다시 촬영하기·저장하기(화면은 SCR-HLT-002), FN-HLT-002-46 가까운 예정 일정 확인(화면은 SCR-HLT-002)
- API가 남의 것: FN-AIM-009-09·FN-HLT-002-46 검진기록·일정 등록 (JAJ), FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 지출 생성·중복 확인 (SJM)
- 함께 만드는 것: `global/external`(OCR 호출 FN-AIM-008-06), `global/idempotency`(멱등 요청, nct-backend에서 가져옴)
- 기다리는 것: health 일정·검진기록 등록 (JAJ), expense 지출 생성·중복 확인 (SJM), OCR 외부 API. 영수증 저장 위치는 docs/OPEN_ISSUES.md에서 정한다
- 완료 조건: 업로드가 jpg·jpeg·png·webp 10MB 이하만 받고 RECPT에 원본을 남긴다. 저장이 반려동물별 검진기록·가계부를 한 트랜잭션으로 만들고 같은 멱등 요청 ID는 한 번만 처리한다
- 연결 남음: FN-AIM-009-09·FN-HLT-002-46 검진기록·일정 등록과 앞뒤 7일 일정 조회 (JAJ — 지금은 `CheckupRecordFake`)
- 연결 남음: FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 지출 생성·중복 확인 (SJM — 지금은 `ExpenseFake`)
- 달라진 것: `global/idempotency`는 가져오지 않고 `RECPT.IDMPT_DMND_ID` 유일 키로 멱등 처리했다. 저장 전 확인·저장 API는 `ReceiptSaveController`

## SCR-AIM-008 영수증 저장 (3차: 가계부 사용처) — 끝
- 기능: FN-AIM-009-10 가계부 기록, FN-AIM-009-23 가계부 중복 확인, FN-HLT-002-41 가계부에도 기록(화면은 SCR-HLT-002), FN-HLT-002-44 가계부 중복 확인(화면은 SCR-HLT-002)
- API가 남의 것: 지출 생성·중복 확인 (SJM — 지금은 `ExpenseFake`)
- 연결 남음: FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 (지출 생성·중복 조회에 사용처 `expndPlcusNm` 전달, SJM — 지금은 `ExpenseFake`)

## SCR-AIM-011 AI 대화창 (대화 저장·조회 내부 API) — 끝
- 기능: FN-AIM-011-07 새 대화 생성(첫 메시지와 함께 저장), FN-AIM-011-08 반려동물 선택·펫 태그(목록 필터·소유 검사), FN-AIM-011-09 대화 목록 조회, FN-AIM-011-10 대화 선택·이어 쓰기(상세 조회·메시지 추가), FN-AIM-011-11 대화 제목 암호화 저장·200자 검증, FN-AIM-011-13 대화 삭제(물리 삭제), FN-AIM-011-14 대화 이력 서버 저장, FN-AIM-011-19 이력 보관·파기(탈퇴 회원 보관일수 경과 후)
- 함께 만드는 것: `ai` 도메인 패키지 신설(층 표 3층, `ArchitectureTest`), `/internal/ai/**` 경로
- 기다리는 것: pet 소유 검사(`global/auth` PetAuthorizer로 가능), member 탈퇴 일시(YAY)
- 완료 조건: 대화 저장·목록·상세·삭제가 본인 것만 동작하고 남의 대화는 404 `NOT_FOUND`다. 제목·내용은 암호문으로 저장된다. 보관일수가 지난 탈퇴 회원의 대화가 파기된다
- 연결 남음: Python AI 서버가 이 API로 DB 직접 접근을 바꾼다(ai 저장소, 2026-10-26 결정)

## SCR-MYP-010 픽셀 캐릭터 관리 · 파일 저장 외부 디렉토리·OS별 기본 경로·서빙 — 끝
- 기능: 없음(연결 작업, 명세 기능 ID 추가 없음). 파일 저장 루트를 `/home/ggorilab/attachment`로 옮기고, 개발 OS(Windows·Mac)별 기본 경로와 공개 이미지 서빙을 만든다. 프론트 LJS 세션 요청(2026-10-27)
- 구현 방식(처음 요청과 다름): `WebConfig` 리소스 핸들러가 아니라 번호로 여는 `FileController`(`GET /api/files/public/{atchFileSn}`)가 `public/` 경로 접두어로 공개 여부를 판정한다. 비공개 범용 `GET /api/files/{fileSn}`은 만들지 않는다(`global`은 소유 검사를 못 함). 비공개는 도메인 API(`/receipts/{recptSn}/image`, `/pets/{petSn}/character-image`, `/items/{itemSn}/image`)가 소유 검사 후 준다. 보안은 `permit-all-paths`에 `/api/files/public/**`만 추가. 개발 Linux는 기본 경로 없음(키 없으면 시작 실패), 경로 철자 `attachment`, Windows는 프로젝트 드라이브 루트 아래, Mac은 `/Users/Shared/ggorilab/attachment`, prod는 키 필수·쓰기 실패 시 시작 중단
- 기다리는 것: 공개·비공개 분류 확인(반려동물·캐릭터 공동양육자 한정 여부 SJM), 운영 서버 폴더 준비와 nginx `client_max_body_size`(10MB 이상)
- 완료 조건: 시작 로그에 해석된 절대 경로가 한 줄 나온다. 공개 이미지가 `/api/files/public/{atchFileSn}`로 열리고, 공개가 아니거나 없는 번호는 404 `FILE_NOT_FOUND`다(비공개는 도메인 API로). prod에서 폴더에 쓸 수 없으면 앱이 시작되지 않는다. Windows에서 프로젝트를 C:와 D:에 두고 띄우면 각 드라이브 아래에 폴더가 생긴다
- 연결 남음: 게시글·캐릭터 이미지를 `<img src="/api/files/public/{atchFileSn}">`로 전환(프론트 연결 공통 파일 이미지 컴포넌트, 프론트 저장소 LJS 마일스톤. 게시글·캐릭터는 담당 KSJ·SJM에게 안내. 캐릭터는 응답에 번호가 없어 `character-image` API 사용). 공개·비공개 분류 확인(SJM), 운영 서버 폴더·nginx `client_max_body_size`(사람)

## 제외
- FN-COM-002-01·03·04·05·06·07·08·10·12·13·14·16·17·18·19·20·21·22·23: 화면만 있다(서버 동작 없음)
- FN-COM-002-11: 채팅 안 읽은 수 API는 KSJ(채팅, 마일스톤 담당 표 COM-009)
- FN-COM-003-02·03·04·06·08·09: 화면만 있다(복귀 파라미터·랜딩 이동은 프론트)
- FN-COM-003-05: 로그인 처리는 YAY(로그인·로그아웃 API). 복귀 이동은 화면만
- FN-COM-003-10: AI 대화창 세션은 Python AI 서버와 프론트(이 저장소 아님)
- FN-COM-004-01·15·16: 화면만 있다
- FN-COM-004-14: 채팅방을 보는 동안 거래 알림을 만들지 않는 판단은 알림을 발행하는 채팅 쪽 KSJ
- FN-COM-005-01·03·04·05·06·08·10: 화면만 있다
- FN-COM-005-07: 로그아웃 API는 YAY(`POST /auth/logout`)
- FN-COM-005-09: AI 대화 이력은 Python AI 서버(이 저장소 아님)
- FN-COM-006-01~09: 화면만 있다(GNB 2차 메뉴)
- FN-COM-007-01~07: 화면만 있다(AI 새 창 런처)
- FN-COM-008-01·15·17·18·21: 화면만 있다
- FN-COM-009-01~20: 채팅 모달 API는 KSJ(마일스톤 담당 표 COM-009)
- FN-AIM-011-01~06·17·18·20: 화면만 있다(새 창, 사이드바, 창 종료·세션 공유는 프론트와 Python 미들웨어)
- FN-AIM-011-15·16: AI 컨텍스트 범위·개인 데이터 비전달은 Python AI 서버(이 저장소 아님)
- FN-AIM-001-01~06, FN-AIM-002-01~11, FN-AIM-004-01~13, FN-AIM-005-01~07: AI매니저 대화 API는 Python AI 서버(이 저장소 아님, 마일스톤 담당 표 LJS 줄)
- FN-AIM-012-01~12·15~18: AI매니저 대화의 일정 생성 확인은 Python AI 서버(이 저장소 아님)
- FN-AIM-012-13·14: 일정 저장은 JAJ(`health` 일정 등록 재사용, 마일스톤 담당 표 AIM-012-13·14)
- FN-AIM-006-08: SNS 가입 이메일 확보는 YAY(가입), 발송 쪽 서버 동작 없음
- FN-AIM-008-01: 진입 버튼과 등록 방법 선택은 JAJ(FN-HLT-002-33)
- FN-AIM-008-08: 직접 입력은 JAJ의 검진기록 등록·수정 모달
- FN-AIM-009-05·06·14·15·25: 화면만 있다(행 추가·삭제, 점선 표기, 다시 촬영, 저장 안내)
- FN-AIM-010-02·03: 화면만 있다(실패 안내, 재촬영 전환)
- FN-AIM-010-04: 직접 입력은 JAJ의 검진기록 등록·수정 모달(FN-HLT-002-26~31)
- FN-MYP-001-02~05: 마이페이지 홈은 SJM(캐릭터 얼굴인 001-01만 LJS)
- FN-MYP-002-01·03~08: 반려동물 프로필은 SJM(캐릭터 얼굴인 002-02만 LJS)
- FN-MYP-003-01~07, FN-MYP-004-01~04, FN-MYP-005-01~08, FN-MYP-006-01~05: 반려동물·공동양육은 SJM
- FN-MYP-010-05: 대표 반려동물 지정은 SJM(마일스톤 담당 표 "MYP-010, 05 제외")
- FN-MYP-010-15·22: 화면만 있다(상점 열기, 0마리 안내)
- FN-MYP-011-01·04: 화면만 있다(임시 착용 미리보기, 담은 목록)
- FN-MYP-020-01~10: 포인트·케어미션은 SJM
- FN-MYP-030-01~21, FN-MYP-031-01~06: 가계부는 SJM
- FN-MYP-040-01~12: 회원정보 수정은 SJM
- FN-HLT-002-01·05·06·09~33, FN-HLT-003-01·12·13·21~24·29~40, FN-HLT-005-01~05·11~15, FN-HLT-007-01~06, FN-HLT-011-01~10: 건강기록은 JAJ(영수증 결과 확인인 002-34~46만 LJS)

## 대조
- 공통레이아웃: 명세 117 = 마일스톤 35 + 제외 82
- AI매니저: 명세 138 = 마일스톤 61 + 제외 77
- 마이페이지: 명세 112 = 마일스톤 23 + 제외 89
- 건강기록: 명세 86 = 마일스톤 13 + 제외 73
