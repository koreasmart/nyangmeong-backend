# 공통(global) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /common-codes | 공통코드 하위 목록 조회 `?prntComCd=PETG03` *통합 때 추가* | MYP-003-03 | LJS | - | List<CommonCodeSummary> |
| GET | /files/public/{atchFileSn} | 공개 이미지 파일(게시글 사진·생성된 캐릭터 이미지). 로그인 없이 열린다. 공개가 아니거나 없는 번호는 404 `FILE_NOT_FOUND`. 응답의 `atchFileSn`을 그대로 `<img src="/api/files/public/{atchFileSn}">`에 쓴다 | 연결 작업(파일 저장 외부 디렉토리) | LJS | - | 이미지 파일 |

## DTO

### CommonCodeSummary
쓰는 API: GET /common-codes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| comCdSn | Long | Y | | COM_CD.COM_CD_SN |
| comCd | String | Y | 코드값(8자) | COM_CD.COM_CD |
| comCdNm | String | Y | 화면에 보이는 이름 | COM_CD.COM_CD_NM |
| sortSeq | Integer | Y | 이 순서로 정렬해 돌려준다 | COM_CD.SORT_SEQ |
