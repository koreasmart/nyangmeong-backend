# 냥멍케어 백엔드 컨벤션: API

`AGENTS.md`에서 이어지는 문서다.

- 접두어는 `/api`이다. 버전(`/v1`)은 경로에 넣지 않는다. 리소스는 복수형 kebab-case로 쓴다. 식별자는 경로에, 필터·정렬·페이지는 쿼리에 둔다.
  - 예: `/members/me`, `/pets/{petSn}/checkups`, `/places?plcClsfCd={공통코드}&keyword=병원`
  - 예외: `/internal`은 서버 간 내부 호출(Python AI 서버 → Spring) 전용이다. 서버 간 키는 없다(2026-10-26 결정). 그래서 `/internal/**`의 모든 API는 사용자 인증(쿠키)을 거치고 호출한 회원 자신의 정보만 다룬다. 사용자 인증 없이 회원을 지정하는 API는 `/internal`에 두지 않는다
- 로그인한 회원 자신의 자원은 `me`를 쓴다(`/members/me`). 경로에 회원 일련번호를 넣지 않는다.
  - 회원에 딸린 설정·단건(회원당 하나)은 `/members/me/x`에 둔다. 예: `/members/me/notification-settings`, `/members/me/points`, `/members/me/expense-limit`
  - 회원이 쌓는 목록 자원은 최상위 경로에 두고 서버가 로그인 회원으로 거른다. 예: `/notifications`, `/expenses`, `/point-ledgers`, `/chat-rooms`
- 반려동물에 속한 자원은 `/pets/{petSn}/...` 아래에 둔다(예: `/pets/{petSn}/checkups/{ckupRcdSn}`). `PetAuthorizer`가 `petSn`으로 권한을 검사하기 때문이다.
- 댓글이나 첨부처럼 부모 없이 존재할 수 없는 자원은 부모 아래에 한 단계까지 중첩할 수 있다(예: `/posts/{pstSn}/comments`). 그 밖의 자원은 중첩하지 않고 쿼리로 거른다.
- 쿼리 파라미터는 camelCase로 쓴다. 컬럼에 해당하는 값은 컬럼 이름을 그대로 쓰고(`plcClsfCd`), 검색어는 `keyword`로 통일한다.
- 경로 변수는 그 테이블의 일련번호 컬럼 이름을 camelCase로 쓴다. `{id}`를 쓰지 않는다. 예: `/posts/{pstSn}`, `/pets/{petSn}/checkups/{ckupRcdSn}`
- 상태를 바꾸는 동작은 동사 하위 경로로 쓴다. 예: `POST /care-matches/{petPrtcMtchSn}/accept`

| 동작 | 메서드 | 상태 코드 |
| --- | --- | --- |
| 조회 | GET | 200 |
| 생성 | POST | 201 |
| 수정 | PATCH | 200 |
| 목록·설정 전체 저장 | PUT | 200 |
| 삭제 | DELETE | 200 |
| 상태 전이 | POST | 200 |

- 204는 쓰지 않는다. 삭제도 `ApiResponse`를 반환한다.
- 수정은 PATCH다. 보낸 필드만 바뀌고 보내지 않은 필드는 그대로다. `null`을 보낸 필드도 보내지 않은 것과 같이 그대로 둔다. 값을 비워야 하는 필드는 빈 문자열(`""`)을 보낸다.
- 생성과 수정은 바뀐 자원을 `XxxResponse`로 돌려준다. 삭제와 상태 변경은 `data`가 `null`이다.
- 단건 조회에서 대상이 없으면 404다. 목록 조회에서 결과가 없으면 200에 빈 `content`를 돌려준다.
- PUT은 목록이나 설정 전체를 한 번에 저장하는 API에만 쓴다(예: 케어리스트 순서 저장, 알림 수신 설정 일괄 저장). 요청에 없는 항목은 지워진다. 그 밖의 수정에는 PUT을 쓰지 않는다.
- DELETE는 본문을 받지 않는다. 일부 클라이언트·프록시가 DELETE 본문을 버리기 때문이다. 여러 건 삭제는 번호를 쿼리로 반복한다(`DELETE /expenses?expndSn=1&expndSn=2`). 조건으로 한꺼번에 지우는 것처럼 본문이 필요하면 `POST /{자원}/bulk-delete`로 둔다.
- 돈·포인트를 만들거나 옮기는 생성 요청은 `idmptDmndId`(클라이언트가 만든 UUID, 최대 100자)를 필수로 받고, 그 값을 유일 키 컬럼에 저장해 막는다. 같은 `idmptDmndId`를 다시 보내면 200과 처음 결과를 같은 모양으로 돌려준다. 테이블에 자연 유일 키가 있으면(예: 케어 완료의 항목+날짜, 좋아요의 게시글+회원) 그 키로 막고 `idmptDmndId`를 받지 않는다.
- 켜고 끄는 동작(좋아요 등)은 멱등이다. 이미 켜진 상태의 POST, 이미 꺼진 상태의 DELETE도 200이다.

오류의 상태 코드는 `ErrorCode`를 추가할 때 아래에서 고른다.

| 상황 | 상태 코드 | 예 |
| --- | --- | --- |
| 입력값이 틀렸거나 업무 규칙에 어긋남 | 400 | `PET_LIMIT_EXCEEDED` |
| 토큰이 없거나 틀림, 만료됨(이 경우에만) | 401 | `UNAUTHORIZED`, `TOKEN_EXPIRED` |
| 권한이 없음, 정지·탈퇴 회원 | 403 | `PET_PRIMARY_ONLY`, `MEMBER_INACTIVE` |
| 대상이 없음 | 404 | `NOT_FOUND` |
| 이미 있음 | 409 | `EMAIL_ALREADY_REGISTERED` |
| 로그인 5회 실패로 보안코드 확인이 필요함 | 429 | `LOGIN_CAPTCHA_REQUIRED` |

보호 파일 다운로드(파일 자체를 돌려준다)를 제외한 모든 응답은 아래 모양이다(2026-10-20 확정, nct-backend와 같다). 프론트는 `status`로 성공을 판정하고 `data`를 꺼내며 `code`로 분기한다. 바꿀 때는 프론트 저장소의 `docs/convention/api.md`와 같이 고친다.

```json
{"timestamp":"2026-10-20T09:10:00","status":"success","httpCode":200,"message":"ok","data":{ ... }}
{"timestamp":"2026-10-20T09:10:00","status":"success","httpCode":201,"message":"created","data":{ ... }}
{"timestamp":"2026-10-20T09:10:00","status":"error","httpCode":404,"message":"일치하는 회원정보를 찾을 수 없습니다.","path":"/api/auth/me","code":"MEMBER_NOT_FOUND"}
{"timestamp":"2026-10-20T09:10:00","status":"error","httpCode":400,"message":"입력값이 올바르지 않습니다.","path":"/api/pets","code":"INVALID_INPUT_VALUE","data":[{"field":"petNm","message":"이름을 입력해 주세요"}]}
```

- null 필드는 JSON에서 빠진다. 성공 응답에는 `path`·`code`가 없고, `data`가 없는 성공(삭제·상태 변경)에는 `data`도 없다.
- 응답의 `code`는 `ErrorCode` 상수 이름(`errorCode.name()`)이다. `message`는 문구가 바뀔 수 있으므로 프론트는 `code`로만 분기한다. 자바의 `ErrorCode.code()`는 HTTP 상태 숫자(400)라 응답 `code`와 뜻이 다르다. 응답을 만들 때는 `ApiResponse.error(errorCode, path)`를 쓰고 두 값을 손으로 조합하지 않는다.
- 입력값 검증 오류(`INVALID_INPUT_VALUE`)만 `data`에 `[{"field":"...","message":"..."}]`를 담는다.
- 컨트롤러는 `ResponseEntity<ApiResponse<T>>`를 돌려준다(`ResponseEntity.ok(ApiResponse.success(data))`, 생성은 `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data))`). 실패 응답은 `GlobalExceptionHandler`와 `global/security`의 응답기가 만들며 컨트롤러가 직접 만들지 않는다.
- 인증 토큰(Access·Refresh)은 httpOnly 쿠키로만 오간다. 응답 본문에 토큰을 넣지 않는다. 토큰이 만료되면 401 `TOKEN_EXPIRED`가 오고 프론트가 `POST /auth/refresh`로 재발급한 뒤 다시 보낸다.
- 상태를 바꾸는 요청(GET·HEAD·OPTIONS가 아닌 요청)은 `X-Requested-With: XMLHttpRequest` 헤더가 없으면 403이 된다. 프론트는 모든 요청에 이 헤더를 붙인다. Swagger UI는 이 헤더를 미리 채운다(`OpenApiConfig`). curl로 시험할 때 403이 나오면 이 헤더부터 확인한다.

날짜와 시간은 아래 형식으로만 주고받는다.

- 일시(`LocalDateTime`)는 `"2026-10-12T09:30:00"`, 날짜(`LocalDate`)는 `"2026-10-12"`다. Spring Boot의 기본 형식(ISO 8601)이므로 `@JsonFormat`을 붙이지 않는다.
- 시간대 표기(`Z`, `+09:00`)는 붙이지 않는다. 값은 모두 한국 시간(`Asia/Seoul`)이다.
- DTO의 일시·날짜 필드는 `String`이 아니라 `LocalDateTime`·`LocalDate`로 둔다.
- 쿼리 파라미터의 날짜도 같은 형식으로 받는다. 공통 설정(`spring.mvc.format.date: iso`, `date-time: iso`)이 변환하므로 `@DateTimeFormat`을 붙이지 않는다.

목록 API는 요청과 응답의 모양이 모두 같다.

- 요청: `?page=1&size=20&sort=...`. `page`는 1부터 시작한다. `size`는 최대 50이다.
- `size`를 생략하면 20건이다. 게시판 10건, 실종 카드 12건처럼 화면설계서가 정한 값이 있으면 서비스가 `pq.sizeOr(10)`처럼 그 값을 기본값으로 넘긴다.
- 응답의 `data`: `content`(목록), `page`, `size`, `totalElements`, `totalPages`, `hasNext`.
