# 설정과 비밀값

설정 파일, 비밀값, 환경변수를 다룰 때와 핸드오프를 쓸 때 연다.

**이 저장소는 공개다. 한 번 커밋된 값은 지워도 이력에 남는다.**

- 스프링 설정은 `src/main/resources/application.properties` 하나에 둔다. 비밀값(DB 접속 정보, JWT·암호화 키, 메일 비밀번호, 소셜 로그인 시크릿, 외부 API 키)이 들어 있어 git에 올리지 않고(`.gitignore`) 디스코드로 공유한다. 서버 주소도 공개 저장소에 적지 않는다.
- 키 목록은 커밋된 `application.properties.example`에 둔다. 값은 비우고, 비밀값을 쓰지 않는다. 키를 추가·변경하면 이 예시 파일도 같은 PR에서 고치고 디스코드의 파일도 갱신한다.
- DMS MCP 서버 주소(`dms.mcp-url`)도 같은 파일에 둔다. `setup.sh`가 읽어 감지한 AI 도구(Claude Code·Codex)에 등록한다.
- 운영 프로필은 `application-prod.properties`(비밀값 없음, 커밋)에 둔다. 개인 로컬 설정은 `application-local.properties`(git 제외)에 둔다.
- 테스트 설정은 `src/test/resources/config/application.yml`이다. main의 `application.properties`가 없는 PC에서도 테스트가 돌도록, 테스트에 필요한 공통 설정은 여기에도 둔다.
- 로그에 비밀번호와 토큰을 남기지 않는다.
- 비밀값이 든 파일(`application.properties`, `.env` 등)과 `CLAUDE.local.md`는 git에 올리지 않는다.
- 작업 기록은 `handoff/<이름>.md`에 사람마다 한 파일로 두고 커밋한다. 다른 PC에서도 이어서 작업하기 위해서다. 공개 저장소이므로 비밀값과 개인 PC 경로를 쓰지 않는다.
