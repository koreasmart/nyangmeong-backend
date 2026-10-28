# 여러 명이 같이 고치는 파일

`ErrorCode`, `application.properties.example`, `db/`의 SQL을 고칠 때 연다.

`ErrorCode`와 `application.properties.example`(실제 `application.properties`는 git 제외, 디스코드로 공유)은 모든 도메인 담당이 줄을 추가한다. 다들 맨 끝에 붙이면 병합할 때마다 충돌한다.

- 파일 안을 도메인별 구획으로 나누고(주석으로 구분), 자기 도메인 구획 안에 추가한다.
- 이 파일들의 줄 정렬이나 순서 바꾸기를 기능 PR에 섞지 않는다.

`db/`의 SQL 파일도 여러 명이 추가한다.

- 테이블 변경은 `db/migration/`에 추가한다. 파일 이름은 `YYYYMMDD_NN_설명.sql`이다(예: `20261022_01_add_expnd_idmpt_dmnd_id.sql`, `NN`은 같은 날짜 안의 순번). 자세한 규칙은 `db/migration/README.md`에 있다.
- `db/` 바로 아래는 처음부터 새로 만드는 기준 파일(전체 DROP 후 생성, 공통코드 시드)만 둔다. 공용 DB에는 실행하지 않는다.
- 병합된 SQL 파일은 고치지 않는다. 바꿀 것이 있으면 새 파일을 추가한다.
- 테이블을 바꾸는 PR은 본문에 "DB 변경 있음"과 파일 이름을 적는다.
- PR 작성자가 병합 직후 팀 공용 DB에 그 SQL을 실행하고 PR에 완료 댓글을 남긴다.

시드 데이터 생성기(`src/seed`, README "시드 데이터")는 테이블 구조를 따라간다.

- 테이블을 바꿔도 생성기는 고치지 않아도 돈다(자동 엔진이 빠진 값·없어진 컬럼·새 테이블을 메우고 "자동 처리" 경고를 남긴다). 새 테이블이나 컬럼에 업무 규칙(상태 이력, 개수 상한 등)이 있어 자동 값이 화면에 어색하면 그 도메인 담당이 `src/seed/.../domain`의 Seeder에 값을 더한다.
- 공용 DB 시드는 시드 대상 테이블이 모두 빈 상태에서 넣는다. 넣기 전에 팀에 알린다.
- 공용 DB 시드를 고쳐 다시 넣을 때는 팀에 알린 뒤 `./gradlew seedClean -Ptarget=shared` → `seedData` 순서로 한다. `seedClean`은 팀원이 만든 테스트 데이터까지 지운다.
