# 냥멍케어 백엔드 컨벤션

이 저장소에서 코드를 쓰는 사람과 AI 도구가 따르는 규칙이다. 이 파일에는 항상 필요한 규칙만 두고, 나머지는 `docs/convention/`에 있다("나머지 규칙이 있는 곳").

- Codex는 이 파일을 자동으로 읽는다. Claude Code는 `CLAUDE.md`의 `@AGENTS.md` 한 줄로 읽는다. 그 줄을 지우면 규칙이 통째로 빠진다.
- 이 파일은 AI 도구가 매번 통째로 읽으므로 짧게 유지한다. 특정 작업에만 필요한 규칙은 `docs/convention/`에 쓴다.
- Git 규칙(`docs/convention/git.md`)은 팀 합의 전 제안이다. 아직 정하지 않은 것은 `docs/OPEN_ISSUES.md`에 있다.

## 코드를 쓰기 전에 꼭 알 것

어기면 되돌리기 어렵거나 오류 없이 조용히 잘못되는 것만 모았다. 나머지는 필요할 때 찾아본다.

1. **비밀값을 커밋하지 않는다.** 이 저장소는 공개다. 비밀번호, 키, 토큰은 git에 올리지 않는 `application.properties`(디스코드로 공유)에만 넣는다. (`docs/convention/config.md`)
2. **의존은 한 방향이다.** 도메인이 `global`을 쓰고, 다른 도메인은 `service`로만, 자기보다 낮은 층만 부른다. 순환 참조 오류가 나면 `@Lazy`나 `ObjectProvider`로 덮지 않고 이벤트로 바꾸거나 기능을 위층으로 옮긴다. (의존 방향)
3. **알림과 메일은 `@Transactional` 메서드 안에서 `EventPublisher.publish()`로 발행한다.** 트랜잭션 밖에서 부르면 예외가 난다. `ApplicationEventPublisher`를 직접 쓰면 이 검사가 빠져서 오류 없이 사라진다. (`docs/convention/service.md`)
4. **테스트를 끄거나 지워서 통과시키지 않는다.** (`docs/convention/test.md`)
5. **`DEL_YN`이 있는 테이블을 조회할 때는 `DEL_YN = 'N'` 조건을 넣는다.** 빠뜨리면 삭제된 데이터가 오류 없이 화면에 나온다. 삭제는 `DELETE`가 아니라 `DEL_YN = 'Y'`로 바꾸는 `UPDATE`다. (`docs/convention/mapper.md`)

## 기술 스택과 편집기

Java 25, Spring Boot 4.1, Spring Security 7, MyBatis(Mapper 인터페이스 + XML), MySQL 8, Lombok, Gradle. Docker는 쓰지 않는다.

- VS Code나 STS로 작업한다. Java 포맷은 두 IDE 모두 `eclipse-formatter.xml`(공백 4칸)을 쓰고 저장할 때 적용된다. 개인 설정으로 덮어쓰지 않는다. 줄바꿈은 LF다.
- Mapper XML은 자동 포맷하지 않는다. 포매터가 SQL의 줄 배치를 바꾸기 때문이다.
- 그 밖의 IDE를 쓰면 파일 전체 포맷을 하지 않는다. 포매터가 달라서 다른 사람이 쓴 줄까지 바뀐다.

## 패키지 구조

```
com.nyangmeong
├─ global/                 공통 모듈. 도메인을 모른다
│  ├─ response/  error/  security/  auth/  validation/  file/
│  ├─ notification/  mail/  code/  setting/
│  └─ external/  batch/  config/  log/
├─ member/  auth/  pet/  health/  community/  place/
├─ support/  notification/  expense/  ai/
│                          업무 기능. 도메인 1개 = 패키지 1개
├─ admin/                  여러 도메인을 합치는 관리자 조회만
└─ scheduler/              여러 도메인에 걸친 배치만

src/main/resources/mapper/{도메인}/{이름}Mapper.xml   (공통 모듈은 mapper/global/)
db/                        테이블 생성 기준 SQL (저장소 루트). 사람이 직접 실행한다.
db/migration/              기존 DB 위에 차례로 실행하는 변경 SQL. 공용 DB에는 이것만 실행한다
```

도메인 안은 다섯 패키지로 통일한다. 하위 패키지, 깊이, 설정 클래스 위치는 `docs/convention/architecture.md`에 있다.

| 패키지 | 넣는 것 | 넣지 않는 것 |
| --- | --- | --- |
| `controller` | `@RestController`, 요청 검증, `ApiResponse` 반환 | 비즈니스 분기 |
| `service` | `@Transactional`, 권한 검사, 도메인 규칙, 이벤트 발행 | `HttpServletRequest` |
| `dto` | 요청·응답·검색 조건 | 테이블 1:1 VO(`domain`에 둔다). VO를 응답으로 그대로 반환하지 않는다 |
| `mapper` | MyBatis 인터페이스 | (`docs/convention/mapper.md`) |
| `domain` | 테이블 1:1 VO, 상태·종류 enum | 요청·응답 전용 필드 |

## 의존 방향

1. 도메인은 `global`을 쓴다. `global`은 도메인을 참조하지 않는다. `global`이 테이블을 읽어야 하면 도메인의 Mapper를 쓰지 않고 자기 Mapper를 둔다(`mapper/global/`).
2. 도메인 안에서는 `controller` → `service` → `mapper` 순서로만 부른다.
3. 다른 도메인은 그 도메인의 `service`로만 부른다. 다른 도메인의 `mapper`를 직접 부르지 않는다.
4. 알림과 메일은 호출하지 않고 이벤트를 발행한다.

1~3번, 아래 층 표, `admin`·`scheduler` import 규칙은 ArchUnit 테스트(`ArchitectureTest`)가 검사한다. `./gradlew build`에서 이 테스트가 실패하면 메시지에 나온 import를 고친다. 테스트를 고치거나 꺼서 통과시키지 않는다. 도메인을 새로 만들면 층 표와 이 테스트를 같이 고친다.

### 도메인 간 호출은 낮은 층으로만

| 층 | 도메인 | 부를 수 있는 곳 |
| --- | --- | --- |
| 0 | `member` | 없음 |
| 1 | `pet`, `auth` | 0층 |
| 2 | `health`, `place`, `notification` | 0~1층 |
| 3 | `expense`, `community`, `support`, `ai` | 0~2층 |
| 4 | `admin`, `scheduler` | 전부 |

- 자기보다 낮은 층의 service만 부른다. 같은 층끼리는 부르지 않는다. 이 규칙이면 두 도메인이 서로를 부르는 순환이 생기지 않는다.
- 위층의 기능이 필요하면 위층을 부르지 않는다. 위층이 아래층을 부르도록 방향을 바꾸거나, 이벤트를 발행하거나, 그 기능을 위층으로 옮긴다.
  - 예: 건강기록·가계부·커뮤니티의 활동이 케어미션(`pet`)에 반영되어야 한다. `pet`이 위층을 읽어 오지 않고, 활동이 일어난 도메인이 `pet`의 service를 부른다.
- 순환 참조 오류(`BeanCurrentlyInCreationException`)가 나면 위 방법으로 푼다. `ObjectProvider`, `@Lazy`, setter 주입으로 오류만 숨기지 않는다.
- 다른 도메인의 service는 인터페이스를 따로 만들지 않고 클래스를 바로 주입한다.

## 나머지 규칙이 있는 곳

아래 파일은 자동으로 읽히지 않는다. 해당하는 일을 하기 전에 그 파일만 연다.

| 이럴 때 | 읽는 파일 |
| --- | --- |
| 코드를 쓸 때 항상 | `docs/convention/common.md`: 로그와 예외, 주석, 이름 짓기, 공통 모듈에서 가져다 쓰는 것 |
| Controller를 쓸 때 | `docs/convention/controller.md`, `docs/convention/api.md` |
| API 경로·요청·응답 DTO를 정할 때(기획 포함) | `docs/api/README.md`, `docs/api/<도메인>.md`에서 기능 ID로 검색한 행과 그 DTO. 초안과 다르게 정하면 같은 PR에서 초안도 고친다 |
| Service를 쓸 때 | `docs/convention/service.md` |
| Mapper, Mapper XML, VO, enum을 쓸 때 | `docs/convention/mapper.md` |
| DTO를 쓸 때 | `docs/convention/dto.md` |
| 관리자 기능, 여러 도메인 배치, 하위 패키지를 만들 때 | `docs/convention/architecture.md` |
| 테스트를 쓰거나 실패한 테스트를 고칠 때 | `docs/convention/test.md` |
| 설정 파일·비밀값·환경변수를 다룰 때, 핸드오프를 쓸 때 | `docs/convention/config.md` |
| `ErrorCode`, `application.properties.example`, `db/` SQL을 고칠 때 | `docs/convention/shared-files.md` |
| 브랜치·커밋·PR을 만들 때 | `docs/convention/git.md` |
| 내 담당 화면·API를 마일스톤으로 나눌 때 | `docs/convention/milestone.md` |

## AI 도구 작업 방식

사용량 한도가 있는 플랜을 쓰는 팀원이 있어 토큰을 아낀다.

- 이 파일은 이미 읽혀 있으므로 다시 읽지 않는다. `docs/convention/`은 위 표에서 해당하는 파일만 연다.
- 파일은 필요한 부분만 읽는다. 위치를 모르면 먼저 검색하고 그 주변만 읽는다. `build/`, `bin/`, `.gradle/`은 읽지 않는다.
- 검증 명령은 `./gradlew build -q`다. 실패한 부분만 본다. `/end`가 이 명령의 종료 코드가 0이 아니면 마일스톤을 끝내지 않는다.
- 기능 구현은 기획(`/start`, Opus) → 사람 승인(`승인` 또는 `/impl`) → 구현과 검증 루프(`/impl`: 구현은 Sonnet, 검증은 대화를 보지 않는 별도 에이전트 Opus, `실패`면 고쳐서 최대 3회, 그래도 실패면 사람에게 넘긴다) → 사람 확인(README "사람 확인") → `/end` 순서다. 첫 검증 실패 뒤에는 기준 파일(기획·검증 명령·의존성·테스트)을 고칠 수 없다(`scripts/verify-loop.sh` 훅). 세션을 끊어도 잠금은 보관됐다가 이어지고, 루프를 버리는 것(`clear`)은 사람만 한다. 예외는 `/end`가 사람 확인을 거쳐 마일스톤을 `끝`으로 판정할 때 보관된 루프를 버리는 것이다. 기획이 승인되기 전에는 코드를 쓰지 않는다. 승인된 뒤 사람이 기획에 없는 것을 요청하면 다시 기획하지 않고, 고치기 전에 `handoff/<이름>.md`의 `### 추가 요청`에 먼저 적는다(형식은 `.claude/skills/impl/SKILL.md` 6번). `/impl` 밖의 대화에서 요청받아도 같다. 다른 저장소(프론트↔백엔드) 코드는 내 몫이라도 이 세션에서 고치지 않는다. 기획에 `프론트 연결:`·`백엔드 연결:`로 남기고 그 저장소에서 `/start`로 한다(`docs/convention/milestone.md`). 예외는 `/end`가 `끝` 판정 때 그 저장소의 `milestones/<이름>.md`에 연결 작업 절을 넣는 것 하나다(커밋하지 않는다). 이것도 `/impl` 밖의 대화에서 요청받아도 같다.
- 몇 개 파일로 끝나는 일에는 서브에이전트를 쓰지 않는다. 서브에이전트도 사용량을 쓴다.
- 작업 하나가 끝나면 `/end`로 핸드오프를 남기고 `/clear`한 뒤, 다음 작업은 `/start`로 시작한다.
- 개인 지시는 `CLAUDE.local.md`, 개인 설정은 `.claude/settings.local.json`, 개인 명령은 `~/.claude/skills/my-<이름>/`에 둔다(모두 git 제외). 저장소의 `.claude/skills/`는 공용이라 PR로만 바꾼다.
- 개인 지시는 일하는 방식(설명·기획 형식·말투)만 바꾼다. 기획 승인 → 검증 → 사람 확인 → `/end` 흐름과 이 파일의 규칙이 개인 지시보다 우선한다.
