# 냥멍케어 API·DTO 명세 (초안)

기능명세서에서 뽑은 Spring 백엔드 REST API와 요청·응답 DTO다. **팀 확인 전 초안이다.** 경로·DTO·필드 이름은 명세에 없어 초안 작성 때 지은 것이므로, 각 담당이 자기 도메인 파일을 확인하고 고친다.

필요한 도메인 파일만 연다. 기능 ID로 찾을 때는 그 ID로 검색하면 도메인 파일 API 표의 `기능 ID` 칸에서 나온다.

## 분류표

| 도메인 | 파일 | API | DTO | 담당(API 수) |
| --- | --- | --- | --- | --- |
| 공통(global) | [`global.md`](global.md) | 1 | 1 | LJS 1 |
| 회원(member) | [`member.md`](member.md) | 27 | 28 | YAY 24, SJM 2, LJS 1 |
| 반려동물(pet) | [`pet.md`](pet.md) | 25 | 19 | SJM 16, LJS 8, KSJ 1 |
| 건강기록(health) | [`health.md`](health.md) | 50 | 49 | JAJ 50 |
| 장소(place) | [`place.md`](place.md) | 4 | 4 | YAY 4 |
| 알림(notification) | [`notification.md`](notification.md) | 5 | 4 | LJS 5 |
| 가계부(expense) | [`expense.md`](expense.md) | 10 | 10 | SJM 10 |
| 영수증(expense 도메인) | [`receipt.md`](receipt.md) | 6 | 10 | LJS 6 |
| 커뮤니티(community) | [`community.md`](community.md) | 41 | 35 | KSJ 41 |
| 고객센터(support) | [`support.md`](support.md) | 7 | 7 | KSJ 7 |
| AI 대화(ai, 내부 API) | [`ai.md`](ai.md) | 5 | 5 | LJS 5 |
| 합계 | | 181 | 172 | |

| 그 밖의 파일 | 내용 |
| --- | --- |
| [`issues.md`](issues.md) | 초안을 쓰며 명세만으로 정하지 못한 것 |

## 공통 규칙

- 근거: DMS 기능명세서 9종 최신 판(2026-10-16 확인, 683건), `db/20261012_create_tables_v0.9.sql`(테이블 85개), `docs/convention/api.md`. DTO 필드마다 근거(`테이블.컬럼` 또는 기능 ID)를 적었고, 근거가 없는 필드는 넣지 않았다
- **구현된 API와 DTO는 Swagger(`/swagger-ui.html`)가 정본이다.** 구현 전에는 이 문서를 먼저 고치고 코드를 고친다. 구현하면서 경로·DTO가 이 초안과 달라지면 같은 PR에서 이 문서도 고친다(2026-10-22). 다른 사람이 이 문서를 보고 부를 경로를 정하기 때문이다
- 경로·멱등·다중 삭제 규칙은 `docs/convention/api.md`가 정한다(2026-10-22 보강: `me` 경로, DELETE 본문 금지, 돈·포인트 생성 요청의 `idmptDmndId`, 켜고 끄는 동작의 멱등)
- 경로 앞의 `/api`는 생략했다. 응답은 모두 `ApiResponse`(`{timestamp,status,httpCode,message,path,code,data}`, `docs/convention/api.md`)로 감싸고, 목록(`Page<…>`)은 `PageResponse`(`{content,page,size,totalElements,totalPages,hasNext}`, `content`에 행 DTO)다. 도메인 파일에는 껍데기를 적지 않는다
- 요청·응답 DTO는 `record`다(`docs/convention/dto.md`)
- 필수: 요청은 반드시 보내야 하는 것, 응답은 항상 값이 있는 것(null 아님). 수정(PATCH) 요청은 모두 선택이다
- `Yn` 필드는 이름을 컬럼 그대로 두고 값은 `true`/`false`다. 응답 DTO는 `boolean`, 요청 DTO는 `Boolean`(PATCH에서 보내지 않음을 `null`로 나타낸다). VO는 `String`이고(`docs/convention/mapper.md`), 바꾸는 코드는 응답은 `XxxResponse.from()`에서 `"Y".equals(...)`, 요청은 service에서 `? "Y" : "N"`로 쓴다(`docs/convention/dto.md`)
- 공통코드는 코드값 `String`이다. 코드 값 표현은 `docs/OPEN_ISSUES.md`에서 아직 정하지 않았다
- 암호화 컬럼(`_ENC`)은 복호화한 값을 `Enc`를 뗀 이름으로, 마스킹한 값은 `masked` 접두어로 준다. 비밀번호·토큰 해시는 응답에 넣지 않는다
- "기능 분배 3.1절 N번"은 냥멍케어 기능 분배 문서(2026-10-15 갱신, `milestone.md`의 담당 표가 따르는 문서)의 "두 페이지 이상이 쓰는 기능" 표 번호다

## 요약

API 176개. 담당별: JAJ 50, KSJ 49, YAY 28, SJM 28, LJS 21. 2026-10-19에 AIC 담당 전부를 YAY로, YAY의 알림·영수증 API를 LJS로 옮겼다(`docs/convention/milestone.md` 담당 표). 2026-10-22에 LJS의 구현된 API를 실제에 맞추고(영수증 3 → 6, 상점·아이템 이미지), 지출 조건 전체 삭제를 `POST /expenses/bulk-delete`로 나눴다.
기능 683건의 분류: API 348, FRONT 211, OTHER 46, AISERVER 40, BATCH 17, EVENT 15, UNCLEAR 6.
