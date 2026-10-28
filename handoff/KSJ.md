# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-001 펫이야기 — 끝남 (승인 2026-10-26 17:06, 시작 2026-10-26 16:22, 검증 1회차 통과, 사람 확인 2026-10-27)

기준: 커뮤니티_기능명세서_v0.7_20261019, 커뮤니티_화면설계서_v0.7_20261019, docs/api/community.md, PST·PST_STTS_HSTRY·PST_IMG·PST_RCMDTN·CMNT·DCLR·DCLR_STTS_HSTRY·CMNTY_AI_CMTRY(db/20261012_create_tables_v0.9.sql), 공통코드 CMUG01·02·04·05

만들 파일(`community` 도메인, 3층. 첫 파일이라 패키지를 만든다):
- controller `PostController`(`/api/posts`), `CommentController`(`/api/posts/{pstSn}/comments`), `ReportController`(`/api/reports`), `CommunityAiCommentaryController`(`/api/community-ai-commentaries`)
- service `PostService`, `CommentService`, `ReportService`, `CommunityAiCommentaryService`
- dto `PostCreateRequest`·`PostUpdateRequest`·`PostResponse`·`PostSummary`·`PostSearchCond`·`PostLikeResponse`·`CommentCreateRequest`·`CommentResponse`·`CommentSummary`·`ReportCreateRequest`·`ReportResponse`·`CommunityAiCommentaryResponse`·`ImageItem`
- mapper `PostMapper`·`CommentMapper`·`ReportMapper`·`CommunityAiCommentaryMapper` + `mapper/community/*.xml`. 작성자 닉네임은 XML에서 MBR을 JOIN해 읽는다(읽기 전용, `NotificationMapper.xml`과 같은 방식)
- domain `Post`·`PostImage`·`PostLike`·`Comment`·`Report`·`CommunityAiCommentary`, enum `BoardType`(CMUC0001 펫정보, CMUC0002 펫일상)·`PostStatus`(CMUC0003 정상, CMUC0004 블라인드, CMUC0005 삭제)·`ReportReason`(CMUC0009~0014)·`ReportStatus`(CMUC0015 접수)
- `db/migration/20261026_01_create_pst_inq_hstry.sql`: 조회수 1일 1회용 `PST_INQ_HSTRY`(PST_SN, MBR_SN, INQ_YMD, REG_DT, UK(PST_SN, MBR_SN, INQ_YMD)). 기준 SQL `db/20261012_create_tables_v0.9.sql`에도 같은 표를 넣는다(`docs/convention/shared-files.md` 절차)
- 테스트 `PostServiceTest`·`CommentServiceTest`·`ReportServiceTest`·`CommunityAiCommentaryServiceTest`·`PostControllerTest`

구현 전에 고칠 초안(docs/api/community.md, 같은 PR):
- `PostCreateRequest`·`PostUpdateRequest`: 제목 200자 → **50자**, 내용 4,000자 → **2,000자**(명세 v0.7 FN-CMU-001-14·화면설계서 글쓰기·글수정). `atchFileSns` → **multipart `images`**(List<MultipartFile>, jpg·jpeg·png·webp, 장당 10MB, 최대 10장). 수정은 남길 사진 `keepAtchFileSns` + 새 `images`. 이유는 SCR-CSC-003과 같다(먼저 올려 번호를 받는 공용 업로드 API가 없다). 실종센터 `pstTtl`·`pstCn`의 "(펫이야기와 동일)" 설명도 50·2,000으로 맞춘다
- `CommentCreateRequest`: 1,000자 → **500자**(FN-CMU-001-11). `prntCmntSn`이 대댓글이면 400 대신 **그 대댓글의 최상위 댓글에 붙인다**(화면설계서 상세 "답글에 대한 답글은 원 댓글 스레드에 이어서 노출")
- `ReportCreateRequest`: `dclrRsnCd`가 CMUC0014(기타)면 `dclrDtlCn` 필수(화면설계서 댓글신고모달 3번)
- `PostSummary`: 목록 화면에 나오는 `mbrNcnm`·`regDt`·`thumbImg`(ImageItem, 없으면 null)를 더한다

기능별:
- FN-CMU-001-01 목록: `GET /posts` → `Page<PostSummary>`, PST_STTS_CD=정상, BBS_CD는 CMUG01 두 값, 기본 최신순 10건. 완료 조건 명령: 블라인드·삭제 글이 빠지고 최신순 10건
- FN-CMU-001-02 게시판 필터: `?bbsCd=`(없으면 전체, CMUG01 밖이면 400). 완료 조건 명령: 펫일상 필터 테스트
- FN-CMU-001-03 정렬: `?sort=latest|popular`. 인기순은 기간 제한 없이 RCMDTN_CNT 내림차순, 같으면 최신순. 인기순이면 `popular`는 모두 false. 완료 조건 명령: 인기순 정렬·배지 false 테스트
- FN-CMU-001-04 인기글 배지: 최근 7일 정상 글 중 좋아요 상위 5개(같으면 최신순)에 `popular=true`. 목록 순서는 바꾸지 않는다. 완료 조건 명령: 8일 전 글은 좋아요가 많아도 배지가 없다
- FN-CMU-001-05 오늘의 이야기: `GET /community-ai-commentaries?cmtryCrtrYmd=`(없으면 오늘) → CMNTY_AI_CMTRY에서 그 날짜·USE_YN='Y' 행. 없으면 `cmntyAiCmtrySn=null`, `cmtryCn=SystemSettingService.get()`의 AI_CMTRY_BSC_CN. 매일 자정에 문구를 넣는 배치는 이 마일스톤에 없다. 완료 조건 명령: 행 있음·없음 두 경우 테스트
- FN-CMU-001-06 인기 포토: `GET /posts/popular-photo` → 사진이 있는 펫일상 정상 글 중 최근 7일 → 30일 → 전체 순서로 좋아요 1위 `PostResponse`. 하나도 없으면 `data: null`(화면이 위젯을 숨긴다). 완료 조건 명령: 7일 안에 없으면 30일 글이 나온다, 사진 없는 글은 빠진다
- FN-CMU-001-07 오늘의 건강팁: `GET /posts/health-tips` → 펫정보 정상 글 중 HLTH_INFO_EXPSR_SEQ가 있는 글을 그 순서로 3건. 관리자 선정 기능은 이 마일스톤에 없다(SQL로 값을 넣어 확인). 완료 조건 명령: 순서대로 최대 3건
- FN-CMU-001-08 키워드 검색: `?keyword=`, 제목·내용 LIKE. 제목에 있는 글을 먼저, 그 안에서는 선택한 정렬. 0건이면 빈 Page(안내 문구는 화면). 완료 조건 명령: 제목 일치 글이 내용 일치 글보다 앞에 온다
- FN-CMU-001-09·CMU-002-08·CMU-004-12 좋아요: `POST /posts/{pstSn}/likes` → `PostLikeResponse`, `DELETE /posts/{pstSn}/likes` → 200. `INSERT … ON DUPLICATE KEY UPDATE USE_YN='Y'`의 영향 행 수로 실제로 바뀐 경우에만 RCMDTN_CNT를 ±1 한다. 정상 글이 아니면 `POST_NOT_FOUND`. 완료 조건 명령: 두 번 눌러도 수가 1, 누르지 않은 상태에서 취소해도 200이고 수가 그대로
- FN-CMU-001-10 상세: `GET /posts/{pstSn}` → `PostResponse`(imgs는 DEL_YN='N' SORT_SEQ 순, `liked`·`owner`, `cmntCnt`는 삭제 댓글 제외). 정상 글이 아니면 `POST_NOT_FOUND`. 조회수는 `PST_INQ_HSTRY`에 `INSERT IGNORE`가 들어간 경우에만 INQ_CNT+1(같은 회원 하루 1회). 하단 최신 5건은 화면이 `GET /posts?bbsCd=&size=5`로 부른다. 완료 조건 명령: 같은 회원이 두 번 열면 1, 다른 회원이 열면 2
- FN-CMU-001-11·CMU-002-07 댓글: `GET /posts/{pstSn}/comments` → `List<CommentSummary>`(최상위 REG_DT 순, 각 아래 답글 REG_DT 순. 삭제 댓글은 `delYn=true`에 내용·작성자 null, 답글은 유지). `POST` → 201 `CommentResponse`(500자, 공백만이면 400, 부모가 다른 글이면 `COMMENT_NOT_FOUND`). `DELETE /posts/{pstSn}/comments/{cmntSn}` → DEL_YN='Y', 작성자가 아니면 403 `FORBIDDEN`. 완료 조건 명령: 대댓글에 답글을 달면 최상위 댓글에 붙는다, 삭제 댓글 대체·답글 유지, cmntCnt에서 빠진다
- FN-CMU-001-13·CMU-002-12·CMU-004-12 신고: `POST /reports` → 201 `ReportResponse`(접수 CMUC0015, DCLR_STTS_HSTRY에 null → 접수). `pstSn`·`cmntSn` 둘 다 있거나 둘 다 없으면 400. 같은 대상 중복이면 `REPORT_ALREADY_EXISTS`(UK_DCLR_1·2 위반도 같은 오류로 바꾼다). 대상의 신고가 5건이 되면 같은 트랜잭션에서 글은 **블라인드(CMUC0004)** 로 바꾸고 PST_STTS_HSTRY를 남기며, 댓글은 DEL_YN='Y'로 바꾼다. 명세는 "삭제 처리"지만 공통코드 CMUC0004 설명이 "신고 누적 5회 자동 처리·관리자 해제 가능"이라 글은 블라인드로 한다. 완료 조건 명령: 5번째 신고 뒤 글이 목록·상세에서 빠진다, 같은 회원 두 번째 신고 409, 기타에 상세 없으면 400
- FN-CMU-001-14 작성: `POST /posts`(multipart) → 201 `PostResponse`. 상태 정상, PST_STTS_HSTRY(null → 정상). 사진은 `FileStore.store()`로 저장하고 PST_IMG에 SORT_SEQ 0부터(첫 장이 대표 사진). 11장이면 `POST_IMAGE_LIMIT_EXCEEDED`. 완료 조건 명령: 작성 후 목록 첫 행·상세에 나온다, 11장 400
- FN-CMU-001-15 수정·삭제: `PATCH /posts/{pstSn}`(multipart, 게시판 변경 가능, `keepAtchFileSns`에서 빠진 사진은 DEL_YN='Y', 합계 10장 넘으면 400). `DELETE /posts/{pstSn}` → 상태 삭제(CMUC0005) + 이력. 작성자가 아니면 403 `FORBIDDEN`(화면이 안내 후 상세로 보낸다). 완료 조건 명령: 남의 글 수정·삭제 403, 사진 하나 빼고 하나 더한 뒤 상세에 반영, 삭제 뒤 상세 404

전체 완료 조건 명령: `./gradlew build -q` 통과(ArchitectureTest 포함: community는 global만 쓴다)
화면(Postman, dev-token 1번·2번 회원): 1번 작성(사진 2장) → 목록·상세 → 2번 좋아요 두 번(수 1) → 2번 댓글, 1번 답글 → 1번 댓글 삭제(대체 문구, 답글 유지) → 2번 수정 시도 403 → 회원 5명 신고 후 목록에서 빠짐

이번에 하지 않는 것(다음에 할 것에 남긴다):
- 케어미션·포인트 반영(SJM 대기). 작성·댓글·좋아요 뒤 `pet` service를 부르는 자리만 남기지 않고, 메서드가 생기면 붙인다
- 비로그인 열람. 지금 `SecurityPathProperties`는 경로만 보고 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없다. 이번에는 모두 로그인 필요로 둔다
- 오늘의 이야기 자정 배치, 건강팁 관리자 선정, FN-CMU-001-12 공유하기(화면만)

새 의존성: 없음

## 다음에 할 것
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 공용 DB 확인용 데이터 정리: 펫이야기 확인으로 만든 글·사진·댓글·좋아요·신고·조회 이력, SQL로 바꾼 `REG_DT`·`HLTH_INFO_EXPSR_SEQ`를 확인 쿼리로 대상 확인 뒤 되돌리거나 지운다(자식 행부터)
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
- 공용 DB에 검증하며 만든 확인용 문의 OTO_INQRY_SN=201(첨부 3건, 상태 이력 1건)을 확인 쿼리로 대상 확인 뒤 지운다(자식 행부터)
