# LJS 핸드오프

## 지금 하는 것

SCR-MYP-010 파일 저장 외부 디렉토리·OS별 기본 경로·서빙 — 끝 (2026-10-27, `/start`·`/impl` 검증 2회차 통과, 사람 확인). 프론트 LJS 세션 요청. 저장 루트는 `app.file.root-dir`, 비우면 Windows는 프로젝트 드라이브 루트 아래·Mac은 `/Users/Shared` 아래(`FileRootResolver`, 시작 로그 `[FileRoot]` 한 줄, prod는 쓸 수 없으면 시작 실패). 공개 파일은 `public/` 아래에 저장하고 `GET /api/files/public/{atchFileSn}`로 로그인 없이 연다(게시글 사진·생성 캐릭터 PNG). 기획과 다른 점: 요청한 정적 리소스 핸들러·`WebConfig`·비공개 범용 `/api/files/{fileSn}` 대신 번호 방식 컨트롤러(응답에 경로가 없고 `global`은 소유 검사를 못 해서). 비공개는 각 도메인 API가 준다.

## 다음에 할 것

- 프론트 연결(frontend 세션 회신 완료): 공통 파일 이미지 컴포넌트(공개 `/api/files/public/{atchFileSn}` + 비공개 blob 훅)를 프론트가 만든다. 캐릭터 번호가 필요하다고 하면 응답에 번호 추가를 추가 요청으로 받는다
- 팀에 알린다(파일 저장): 각자 `application.properties`의 `app.security.permit-all-paths`에 `/api/files/public/**` 추가, 기존 `uploads/` 파일은 새 경로로 옮기거나 `seedFiles` 다시. 공용 DB(`pawever`)는 옛 시드라 `public/` 행이 없어 시드 사진이 404 → 다음에 공용 시드를 넣을 때 반영
- 운영(사람): 서버에 저장 폴더와 쓰기 권한, nginx `client_max_body_size` 10m 이상. SJM에게 공개·비공개 분류 확인(반려동물·캐릭터를 공동양육자에게만 보이게 할지)
- AIM-011 연결(ai 세션): Python이 쿠키·`X-Requested-With`를 그대로 넘겨 이 API를 부른 뒤 실제 연결 확인. 이때 탈퇴 보관일수 경계 SQL(`MemberMapper.xml` `selectExpiredWithdrawnMbrSns`)은 mock 테스트라 실제 DB에서 아직 실행하지 않았다
- AIM-011 후속: 미완료 메시지를 완료로 바꾸는 `PATCH …/messages/{aiCnvrsMsgSn}`는 설계만(`docs/api/ai.md`). SCR-AIM-002·012에서 필요해지면 추가
- `app.seed.password` 필수값 제외(2026-10-26 사람 요청, 범위 미정): (가) `/start` 설정 키 경고에서만 빼기(`scripts/start-check.sh:51` 제외 목록) (나) `seedData`도 값 없이(기본 비밀번호가 공개 저장소에 들어감). 추천 (가)
- 팀에 알린다(2026-10-26): 공용 DB(`pawever`) 시드를 `seedClean`→`seedData`로 다시 넣었다(팀원 테스트 데이터 삭제됨, 원인 판단 착오로 불필요했던 작업). 예전 시드는 지금 디스코드 키와 다른 AES 키로 암호화돼 있었다. 각 PC는 `./gradlew seedFiles`를 다시. 로컬 서버는 `spring.profiles.default=local`이라 로컬 DB를 쓴다(로컬 데이터가 예전 키면 `seedData -Ptarget=local -Preset`)
- 디스코드 공유 `application.properties`에 `app.seed.password`(새 값) 추가, `app.internal.*` 줄은 지운다(서버 간 키 제거, 사람). 각 PC 로컬 파일의 `app.internal.inbound-key`도 지운다
- 키 제거가 develop에 반영되면 Python 세션(nyangmeong-ai-8a)에 알린다. 그쪽은 이 알림 뒤에 `X-Internal-Key` 헤더·`INTERNAL_API_KEY` 검사·`BACKEND_INTERNAL_KEY` 정리와 실제 연결 확인(`BACKEND_BASE_URL=http://localhost:8080`)을 한다. 그 전에 Python이 헤더를 먼저 빼도 키 제거된 Spring은 무시한다
- Spring→Python HTTP 클라이언트(OCR·캐릭터 실제 구현)는 키 없이 호출한다. Python의 `INTERNAL_API_KEY` 검사가 지워진 뒤에 붙인다
- 영수증 인식 결과 조회(2026-10-26 승인, 프론트 요청): `GET /api/receipts/{recptSn}` → `ReceiptResponse`. 저장된 `RECPT.OCR_RSPNS_JSON`을 `ReceiptOcrConverter.toResponse`로 다시 가공(오늘 날짜·현재 공동양육 반려동물 기준이라 분석 직후와 달라질 수 있음을 허용, 테스트하며 조정). 남의·없는 영수증은 `RECEIPT_NOT_FOUND`. 대화창에서 분석한 영수증을 본창 건강 일지 "인식 결과 확인" 모달에서 다시 연다. `docs/api/receipt.md`에 행 추가
- PR 병합 뒤 공용 DB에 `db/migration/20261025_01_add_expnd_plcus_nm.sql` 실행하고 PR에 완료 댓글(사람). 그 뒤 시드 `ExpenseSeeder`의 OCR 지출을 사용처 = 병원명, 세부사항 = null로 맞춘다
- 요청자에게 알린다: AI매니저 FN-AIM-009-10·21과 건강기록 FN-HLT-002-41 명세서에 아직 "내용 = 병원명". 사용처로 고치고 SCR-HLT-002에 사용처 칸(선택, 80자, 병원명 기본값)을 넣는다
- KSJ에게 확인: 실종 제보·펫돌봄 모집 글의 `PST.BBS_CD` 규칙이 없어 시드는 펫일상으로 넣었다. 정해지면 `CommunitySeeder`를 맞춘다
- 로컬 테스트 DB `nyangmeong_seedtest`가 필요 없으면 `DROP DATABASE nyangmeong_seedtest`(사람)
- 프론트엔드: `getMe` → `/auth/me` 변경(frontend 세션, 커밋 전)과 아래 프론트 변경을 커밋한다
- 하네스 공통화(2026-10-22)의 `scripts/` 5개 수정을 공용 하네스 PR로 올린다(커밋에서 뺐다)
- 프론트 저장소: 같은 이름 스크립트에 같은 macOS 수정(`$( )` 안 `case (패턴)`, `sed -E`, `sed -i` 대신 임시 파일·`mv`, `{…;}`, 한글 앞 `cut -c` 대신 `sed`)을 적용하고, 커밋 전인 `verify-loop.sh` `scripts` 보호·verify·end 스킬 수정과 함께 프론트 PR
- 하네스 아직 못 돌려 본 것: `pr-prep.sh --allow-dirty`(다음 PR 때), 검증 3회 실패 넘기기(다음 화면 작업)
- ERDCloud `EXPND.IDMPT_DMND_ID`를 `EXPND_YMD` 뒤로, `EXPND.EXPND_PLCUS_NM`을 `DTL_MTTR` 앞으로 옮긴다(화면에서 끌기, 사람)
- 팀에 알린다(2026-10-22): `docs/convention/api.md` 새 규칙 4가지, `docs/api/README.md`(구현하며 달라지면 같은 PR에서 초안도 고친다), `/start`가 `docs/api`를 기능 ID로 찾아 쓴다, 테이블 변경 SQL은 `db/migration/`에(공용 DB에는 이것만 실행)
- YAY에게 알린다: 로그인·SNS 로그인 응답에 토큰을 넣지 않는다(`TokenResponse` 삭제, `/auth/refresh`는 `data` 없음). SNS 흐름을 정할 때 `issues.md` 로그인 2번 2026-10-22 추가 4가지를 함께 정한다
- SJM에게 알린다: 지출 다중 삭제는 `DELETE /expenses?expndSn=`와 `POST /expenses/bulk-delete`(`expectedCnt` 확인, LJS가 더한 안전장치), 지출 등록은 줄마다 `idmptDmndId`. 사용처 `expndPlcusNm`(80자, `EXPND_PLCUS_NM`)을 등록·수정·응답·목록 DTO에 추가(`docs/api/expense.md`), 중복 확인 표시는 "내용 · 사용처"
- KSJ에게 알린다: 채팅 메시지 커서 조회, 실시간 STOMP 제안과 다중 로그인 다섯 가지(`docs/api/community.md` "실시간"). 세션 폐기 때 소켓을 끊는 이벤트는 `LoginSessionManager.revoke`에서 같이 정한다. 좋아요는 멱등
- JAJ에게 알린다: 케어 완료 중복은 `UK_CARE_CMPTN_HSTRY_1`로 200 기존 기록, 검사결과지 분석은 동기+제한 시간과 `anlysRslt`(제안, 확인 필요). 영수증 확인 모달의 사용처 칸은 저장 요청 `expndPlcusNm`(공통, 병원명 기본값, 비우면 사용처 없음)
- 프론트에 알린다: 알림·상점·영수증 API는 `docs/api/notification.md`·`pet.md`·`receipt.md`(구현에 맞춤). 알림 설정 경로는 `/api/members/me/notification-settings`로 바뀌었다. 영수증 업로드는 실패도 201이고 `ocrPrcsSttsCd`·`failRsn`으로 문구를 고른다. 중복 확인에서 [아니오]를 고른 탭은 `expenseYn=false`
- 프론트 `start` 스킬에도 `docs/api`(백엔드 저장소) 기준 줄을 넣을지 정한다
- 정하기: 휴대폰 일정 가져오기(기획에 없음, JAJ 영역). 넣는다면 QR 짝짓기 `.ics` 업로드, iPhone 단축어, 웹 → 휴대폰은 `webcal://` 구독 QR
- 정하기: 파일 저장소 운영(지금 서버 로컬 디스크 폴더. 서버 여러 대·재배포 대비 영구 볼륨 또는 S3), 영상 업로드 형식·용량
- SCR-AIM-008 연결 남음: health 검진기록·일정 등록(JAJ)이 나오면 `expense/service/CheckupRecordFake`를 그 service로(같은 날 병원 일정 연결·새 일정·앞뒤 7일 미연결 일정·처방약 `CARE_RFLT_YN='N'`), 멱등 재전송용 `remember`·`findByReceipt`도 DB 조회로
- SCR-AIM-008 연결 남음: 가계부 지출 생성·중복 확인(SJM)이 나오면 `ExpenseFake`를, pet 연동 반려동물 목록(SJM)이 나오면 `CaregiverPetFake`를 그 service로(그 전에는 FN-AIM-009-17 구역 자동 연결이 안 된다). 지출 생성에 줄마다 `idmptDmndId`, 사용처 `ExpenseCreateCommand.expndPlcusNm`(세부사항은 null)을 넘기고 중복 조회에 `expndPlcusNm`을 채운다
- SCR-AIM-008 연결 남음: OCR 외부 API 규격이 정해지면 `OcrClient` 실제 구현을 두고 `app.ocr.client`를 바꾼다(지금 `FakeOcrClient`)
- SJM에게 확인: 가계부 모달의 영수증 저장을 `ExpenseService`가 직접 하는지. 영수증 쪽이 하라면 `receipt`(4층) 분리를 다시 검토한다(`docs/OPEN_ISSUES.md` 영수증 행)
- SCR-AIM-006 연결 남음: health 병원 일정 조회(JAJ)가 나오면 `scheduler/HospitalScheduleFake`를, pet 연동 양육자 조회(SJM)가 나오면 `scheduler/CaregiverFake`를 그 service로. 그 뒤 FN-AIM-006-11·13 화면과 01·02·20 실제 조회를 공용 DB 병원 일정으로 확인한다
- SCR-AIM-006 연결 남음: 팀이 SMTP 계정을 정하면 `spring-boot-starter-mail`과 `MailSender` SMTP 구현. 그 전까지 메일은 로그만
- SCR-MYP-011 연결 남음: 포인트 조회·차감(SJM, FN-MYP-020)이 나오면 `pet/service/PointFake`를 그 service로. 구매의 `idmptDmndId`를 포인트 원장에 넘긴다
- SCR-MYP-010 연결 남음: FN-MYP-010-24 1:1 문의 작성(SCR-CSC-003, KSJ)이 `PixelCharacterService.receiveErrorInquiry(mbrSn, pixelCharSn, otoInqrySn)`를 부르고 결과 ATCH_FILE_SN을 첨부한다
- SCR-MYP-010 연결 남음: FN-MYP-010-23 관리자 확인 화면(담당 미정)이 `finishErrorCheck(pixelCharSn, regenerate)`를 부른다
- FN-MYP-010-19: 캐릭터 생성 외부 API가 정해지면 `CharacterGenerator` 실제 구현과 베이스 캐릭터 선택 방식을 판단한다. 기본 캐릭터 이미지(`resources/character/default-*.png`)도 디자인 이미지로
- SCR-COM-004 연결 남음: FN-COM-004-08 공동양육 초대 수락·거절(SJM) API가 나오면 알림 행 버튼과 연결하고 초대 알림 `mvmnPath` 형식을 SJM과 맞춘다
- SCR-COM-002 연결 남음: `/auth/me`의 프로필 이미지 URL은 `FileStore`·파일 조회 API 뒤 연결. 지금은 `atchFileSn`만
- 정하기: HLD_ITEM 유일 키 `UK_HLD_ITEM_1`에 DEL_YN이 없다. 보유 아이템 소프트 삭제가 생기면 재구매가 막히므로 그때 키를 고친다
- 팀에 알린다: 배치 패키지 `job` → `scheduler`(기능 분배 정본은 아직 `job`과 AIM-006 담당 YAY), 케어 일정 사전 알림 `CareScheduleAlarmEvent`, 메일 `global/mail/MailSender`(로그만), 스케줄러 `global/batch/SchedulingConfig`
- 팀에 알린다: 알림은 `EventPublisher.publish(new NotificationEvent(...))`(@Transactional 안), 반려동물 권한 `global/auth/PetAuthorizer`, 파일 `global/file/FileStore`, 공통코드 `GET /api/common-codes?prntComCd=`.
- 팀에 알린다: 개인 로컬 DB는 `application-local.properties`(git 제외)에 `spring.datasource.*` 세 줄. `nyangmeong_` 접두어 스키마에 `db/` 기준 SQL 두 개와 `db/migration/`을 차례로 실행해 만든다
- YAY에게 알린다: 로그인·재발급·로그아웃은 `auth/controller/AuthController`에 `LoginSessionManager`의 `issue`·`rotate`·`revoke`로. 새 공개 경로는 `app.security.permit-all-paths`에. 로그인이 생기면 `DevTokenController` 삭제. 스타터 분석(캡챠 LGN_ATMPT_LMT, 회원 조회 ACNT_EML_HMAC, 소셜 SNS_ACNT_LINK)과 nct-backend OAuth2 핸들러·`CustomOAuth2UserService`(위치는 `docs/OPEN_ISSUES.md`)도 넘긴다
- `global/validation`은 처음 필요한 YAY(닉네임·휴대전화·비밀번호 검증)가 요청하면 만든다
- 디스코드 공유 `application.properties`: `app.front-base-url=http://localhost:5173` 추가, 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고 `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다
- 팀 안건: application.properties를 일반 설정(커밋) + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
- 로컬 DB 정리(`nyangmeong_local`): RECPT 1~3·그 RECPT_STTS_HSTRY·ATCH_FILE, RGTR_ID `LJS`인 PET 4~6·BRUP_REL, PIXEL_CHAR 2와 작업·사진·이력, ITEM 1~3·6·7과 HLD_ITEM, ATCH_FILE 1~5, `uploads/` 아래 파일, RFRSH_TKN 회원 1 테스트 행(`REG_DT >= '2026-10-22'`). 확인용 회원 1·SYS_STNG는 둔다
- 프론트엔드 저장소 변경(`src/api/axios.js` baseURL `/api`, `docs/convention/{api,pages}.md`, `scripts/spec-extract.sh`, start 스킬)을 따로 커밋한다
