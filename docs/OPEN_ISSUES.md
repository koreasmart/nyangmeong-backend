# 아직 정하지 않은 것

`AGENTS.md`와 `docs/convention/`의 규칙에서 빠져 있는 결정을 모았다. 정해지면 그 문서에 규칙으로 옮기고 여기서 지운다.

## 팀이 정할 것

| 항목 | 지금은 이렇게 한다 |
| --- | --- |
| 브랜치·커밋·PR 규칙 전체(`docs/convention/git.md`) | 그 제안을 따른다 |
| 개인 브랜치 이름 형식 | 이니셜 대문자 |
| 커밋 설명을 한국어로 쓸지 | 한국어 |
| 응답 JSON에서 코드 컬럼을 `"01"`로 줄지 이름으로 줄지(프론트에 영향). Y/N 컬럼은 `true`/`false`로 정했다(`docs/convention/dto.md`) | 정해지지 않음. DTO가 쌓이기 전에 정해야 한다 |
| 목록의 `sort` 값: 대소문자(`popular` / `POPULAR`), 목록에 없는 값이 왔을 때(400 / 기본 정렬). 프론트에 영향 | 정해지지 않음. 정렬이 있는 목록을 처음 만드는 사람이 프론트 담당과 정한다 |
| service 사이에 넘기는 값처럼 요청·응답·목록이 아닌 DTO의 이름. `docs/convention/common.md`의 이름 짓기는 DTO 접미어를 `Request`·`Response`·`Summary`·`SearchCond`로 한정한다 | 정해지지 않음 |
| VO·Mapper·Service의 **클래스 이름**을 테이블 약어로 지을지(`Pst`), 뜻이 드러나는 영어로 지을지(`Post`). 필드 이름은 DB 컬럼 그대로로 정해졌다 | 뜻이 드러나는 영어 |
| 기능 이슈를 어느 저장소에 둘지(기능 하나가 백엔드와 프론트 양쪽에 걸친다) | 정해지지 않음 |
| `develop`을 `main`으로 올리는 시점과 담당 | 정해지지 않음 |
| 로그인·로그아웃·재발급 API 구현 시점(YAY). 프론트에 영향 | 토큰 기반(JWT httpOnly 쿠키, 검증 필터, `SecurityConfig`)은 `global/security`에 있다(2026-10-20). 로그인 API가 나오기 전에는 `prod`가 아닌 프로필에서만 뜨는 `POST /api/auth/dev-tokens`(회원 번호 → 쿠키)로 개발한다. 로그인이 생기면 `auth/controller/DevTokenController`·`auth/dto/DevTokenRequest`를 지운다. 운영은 반드시 `prod` 프로필로 띄운다. 세션은 `global/security/session/LoginSessionManager`로 다룬다(2026-10-21): 로그인 성공 시 `issue(mbrSn, response)`(RFRSH_TKN 행 + 쿠키 2개), 재발급 `rotate(request, response)`(옛 세션 폐기 + 새 쿠키, 재사용·실패는 `UNAUTHORIZED`와 쿠키 삭제), 로그아웃 `revoke(request, response)`(Refresh 쿠키만으로 폐기, 쿠키가 없어도 예외 없음). 열린 경로는 `app.security.permit-all-paths`(기본값 `SecurityPathProperties`)에 하나씩 적고, 목록 밖은 전부 인증이 필요하다. `/api/auth/login`·`/refresh`·`/logout`은 이미 목록에 있고, 로그인 상태 확인·보안코드처럼 새로 여는 경로는 목록에 추가한다. 로그아웃은 Access 없이 Refresh 쿠키로 동작한다. 폐기된 세션의 Access는 필터가 바로 401로 막는다 |

## 공통 모듈 담당이 정할 것

| 항목 | 영향을 받는 곳 |
| --- | --- |
| Spring Boot 세부 버전 | 전체 |
| 자기 Mapper XML에서 다른 도메인의 테이블을 조회·조인해도 되는지 | 전체. 정해지기 전에는 하지 않는다 |
| 영수증 이미지와 읽은 결과를 어디에 저장할지(AI 서버 또는 `expense`). 지금은 `expense` 도메인에 둔다(LJS, 2026-10-21): 영수증 저장이 health(2층)·expense(3층)를 모두 불러야 해서 3층이어야 한다. RECPT·RECPT_STTS_HSTRY의 controller·service·mapper·VO는 `expense` 다섯 패키지 안에 `Receipt` 이름으로 둔다 | `expense`, AI 서버 |
| 소셜 로그인의 위치(`global/security` 또는 `member`) | `member` |
| 신고 테이블의 소유 도메인 | `community`, `support` |
| `@Scheduled` 클래스의 위치와 이름 | 배치를 만드는 도메인 |
| 메일 패키지와 `MailEvent`, `global`의 `external`·`batch`·`setting` 패키지, `checkOwner`의 이름과 위치(아직 제안) | 이 이름을 쓰는 도메인. 바뀔 수 있다 |
| 범용 파일 조회 API(`GET /files/{atchFileSn}`)를 둘지. 번호만으로 열면 로그인한 누구나 남의 반려동물 원본 사진·영수증을 볼 수 있다. 지금은 자원별 경로로만 연다(`/pets/{petSn}/character-image`는 `PetAuthorizer`, `/items/{itemSn}/image`는 상점 공용, 2026-10-21) | 프로필 이미지(FN-COM-002-15), 게시글·문의 첨부, 검진 첨부 |

## 필요해질 때까지 미룬 것

이 규모의 프로젝트에서는 아직 필요하지 않다고 보고 규칙으로 만들지 않았다. 실제로 필요해지면 그때 정한다.

| 항목 | 다시 꺼낼 때 |
| --- | --- |
| 관리자 통계 쿼리를 어떻게 짤지(도메인 service 호출 / 한 쿼리 조인) | 관리자 대시보드를 설계할 때 |
| 배치의 처리·실패 지점 기록과 이어서 처리 | 여러 도메인에 걸친 배치를 처음 만들 때 |
| 케어미션·프로필 체중 반영을 직접 호출에서 이벤트로 바꾸기 | 반영이 실패해 본 작업(체중 기록·지출 등록·글 작성)까지 실패하는 것이 문제가 될 때 |
| service를 조회 전용으로 나누는 규칙 | service 하나가 너무 커질 때 |
## 하네스 개선 후보

하네스(`/start`·`/verify`·`/end`)를 시험하며 나왔지만 치명적이지 않아 미뤄 둔 것. 쓰다가 같은 문제를 다시 겪으면 여기에 적고, 흐름이 멈추거나·검증 없이 잘못된 코드가 커밋되거나·남에게 영향이 갈 때 고친다.

| 문제 | 지금은 이렇게 한다 |
| --- | --- |
| 사람이 일부만 확인했다고 답해도 마일스톤을 `끝`으로 바꿀 수 있다 | `/end`가 확인 못 한 것을 밝히고 되돌릴지 묻는다. 사람이 답한다 |
| `/verify`는 기획 밖 변경을 파일 단위로만 본다. 기획에 없던 동작(예: 인증 방식 추가)은 비고로만 남는다 | `/verify` 비고와 PR 리뷰에서 본다 |
