# 냥멍케어 백엔드 컨벤션: Mapper·XML·VO·enum

`AGENTS.md`에서 이어지는 문서다.

## Mapper와 XML

- SQL은 XML에만 쓴다. `@Select` 같은 어노테이션 SQL을 쓰지 않는다.
- 조회 결과는 `<resultMap>`으로 컬럼과 필드를 짝짓는다(수업 프로젝트와 같다). PK는 `<id>`, 나머지는 `<result>`다. `resultType`에 VO·DTO를 바로 쓰지 않는다. `map-underscore-to-camel-case`를 켜지 않으므로, `resultType`으로 받으면 컬럼이 하나도 매핑되지 않아 행이 있어도 `null`이 된다. 숫자·문자 하나를 돌려주는 조회(`COUNT(*)` 등)만 `resultType="long"`처럼 쓴다.
  - 같은 VO를 돌려주는 select는 resultMap 하나를 같이 쓴다. select가 일부 컬럼만 고르면 나머지 필드는 `null`이다.
- 정렬 컬럼은 enum으로만 받는다. 요청값을 SQL에 그대로 붙이지 않는다.
- 감사 컬럼(`REG_DT`, `MDFCN_DT`, `RGTR_ID`, `MDFR_ID`)은 XML에 직접 적는다. **테이블마다 가진 감사 컬럼이 다르다.** `REG_DT`만 있는 테이블도 있고 `MDFR_ID`가 없는 테이블도 있으므로, ERD에서 그 테이블에 있는 컬럼만 적는다.
  - 네 컬럼이 모두 있는 테이블의 수정 예: `MDFR_ID = #{actorId}, MDFCN_DT = NOW()`
  - 서비스가 `actorId`로 `String.valueOf(memberId)`을 넘기고, 배치는 `"SYSTEM"`을 넘긴다.
- 삭제는 테이블에 `DEL_YN` 컬럼이 있는지에 따라 다르다. ERD에서 확인한다.
  - `DEL_YN`이 있는 테이블은 삭제 API에서 `DELETE`를 쓰지 않고 `UPDATE ... SET DEL_YN = 'Y'`로 처리한다. 이 테이블을 조회하는 SQL에는 `DEL_YN = 'N'` 조건을 반드시 넣는다.
  - `DEL_YN`이 없는 테이블은 `DELETE`를 쓴다. 외래키가 삭제를 막으므로(`ON DELETE RESTRICT`) 자식 테이블부터 지운다.
  - `USE_YN`은 삭제가 아니라 사용 여부다. 삭제 용도로 쓰지 않는다.
  - 탈퇴 30일 파기 배치처럼 실제로 지워야 하는 작업만 `DEL_YN`이 있는 테이블에 `DELETE`를 쓴다.
- 페이지가 있는 목록은 아래 형태로만 쓰고 XML에 `LIMIT`을 직접 쓰지 않는다. 최근 N건이나 1건만 가져오는 조회는 XML에 `LIMIT`을 쓴다. 목록을 조회하는 Mapper는 응답 DTO 형태(`XxxSummary`)로 바로 돌려준다(목록에만 해당한다).

```java
@Transactional(readOnly = true)
public PageResponse<PostSummary> search(PostSearchCond cond, PageQuery pq) {
    PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));   // 바로 다음 select 한 번에만 적용
    List<PostSummary> rows = postMapper.selectList(cond, PostSort.from(pq.sort()));
    return PageResponse.of(new PageInfo<>(rows));
}
// NG: rows.stream().map(PostResponse::from).toList()  -- 전체 건수 유실
```

## VO와 enum

- VO는 부모 클래스 없이 테이블 컬럼만 필드로 가진다.
- Y/N 컬럼과 코드 컬럼은 `String`으로 둔다. TypeHandler를 쓰지 않는다.
- 상태·종류 enum은 `code` 필드와 `getCode()`, `is(String code)`를 가진다.
- 코드 값을 Java와 XML에 직접 적지 않는다. 비교는 enum의 `is()`로 하고, XML에는 서비스가 `getCode()`로 넘긴 값을 `#{mtchDlngSttsCd}`처럼 파라미터로 받는다. Y/N만 `"Y".equals(vo.getDelYn())`, `DEL_YN = 'N'`처럼 직접 쓴다.

```java
public enum MatchStatus {
    REQUESTED("01"), ACCEPTED("02");
    private final String code;
    MatchStatus(String code) { this.code = code; }
    public String getCode() { return code; }
    public boolean is(String code) { return this.code.equals(code); }
}
// 비교: MatchStatus.REQUESTED.is(match.getMtchDlngSttsCd())
// NG: "01".equals(match.getMtchDlngSttsCd())  -- 코드 값 직접 사용 금지
```
