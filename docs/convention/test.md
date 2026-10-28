# 테스트

테스트를 쓰거나, 빌드에서 테스트가 실패했을 때 연다.

- 테스트를 지우거나 끄거나 기대값을 바꿔서 통과시키지 않는다. 가끔 실패하는 테스트도 꺼서 넘기지 않고 원인을 고친다.
- PR은 `./gradlew build`가 통과해야 병합한다. `-x test`로 테스트를 건너뛰지 않는다. CI가 설정되기 전에는 PR을 올리는 사람이 로컬에서 돌린다.
- 컨트롤러 테스트를 쓸 때는 `@WebMvcTest`와 `@WithMockLoginMember`를 쓴다.
- DB에 붙는 자동 테스트는 만들지 않는다. 팀이 같이 쓰는 통합 DB에 붙는 테스트는 다른 사람의 데이터를 바꾸고, Docker를 쓰지 않으므로 Testcontainers도 쓸 수 없다. Mapper는 로컬 MySQL에 붙여 직접 실행해 확인한다.
- `DEL_YN`이 있는 테이블을 조회하는 `<select>`에 `DEL_YN` 조건이 있는지는 Mapper XML을 읽는 테스트(`MapperXmlRuleTest`)가 검사한다(`AGENTS.md` "코드를 쓰기 전에 꼭 알 것" 5번). 이 테스트가 실패하면 SQL에 조건을 넣는다. 삭제된 행까지 읽어야 하는 조회는 이 테스트의 예외 목록에 추가한다.
- 의존 방향은 ArchUnit 테스트(`ArchitectureTest`)가 검사한다. 실패하면 메시지에 나온 import를 고친다(`AGENTS.md` "의존 방향").
