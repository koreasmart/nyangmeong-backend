# 장소(place) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /place-bookmarks | 내 장소 즐겨찾기 목록(`?plcClsfCd=`) | HOS-001-17·29·32 | YAY | - | List<PlaceBookmarkSummary> |
| GET | /places | 장소 검색(카테고리·키워드·지도 범위·정렬, 거리는 검색 시점 지도 중앙 기준). 커뮤니티 지도 선택도 사용 | HOS-001-03~07·16·19·20·30, PLC-001-01, PLC-002-01, PLC-003-01, PLC-004-01, CMU-002-11, CMU-004-10, CMU-005-05 | YAY | - | Page<PlaceSummary> |
| POST | /place-bookmarks | 장소 즐겨찾기 저장 | HOS-001-09·23 | YAY | PlaceBookmarkCreateRequest | PlaceBookmarkResponse |
| DELETE | /place-bookmarks/{plcBkmkSn} | 장소 즐겨찾기 해제 | HOS-001-09·23 | YAY | - | - |

## DTO

### PlaceBookmarkCreateRequest
쓰는 API: POST /place-bookmarks
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| plcSn | Long | Y | 저장된 PLC 행이 있어야 한다(`issues.md` API의 로그인·플레이스·커뮤니티·고객센터 10번) | PLC_BKMK.PLC_SN, HOS-001-09·23 |

### PlaceBookmarkResponse
쓰는 API: POST /place-bookmarks
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| plcBkmkSn | Long | Y | 해제 API 경로 변수 | PLC_BKMK.PLC_BKMK_SN |
| plcSn | Long | Y | | PLC_BKMK.PLC_SN |

### PlaceBookmarkSummary
쓰는 API: GET /place-bookmarks
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| plcBkmkSn | Long | Y | | PLC_BKMK.PLC_BKMK_SN |
| plcSn | Long | Y | | PLC_BKMK.PLC_SN |
| plcClsfCd | String | Y | 공통코드 PLCG01 | PLC.PLC_CLSF_CD |
| plcClsfCdNm | String | Y | 업체 유형 표시 | COM_CD.COM_CD_NM, HOS-001-07 |
| plcConm | String | Y | | PLC.PLC_CONM |
| plcAddr | String | Y | | PLC.PLC_ADDR |
| rprsTelno | String | N | | PLC.RPRS_TELNO |
| plcLat | BigDecimal | Y | "현재 지도 밖" 판단에 쓴다 | PLC.PLC_LAT, HOS-001-32 |
| plcLot | BigDecimal | Y | | PLC.PLC_LOT, HOS-001-32 |
| kakaoPlcId | String | N | | PLC.KAKAO_PLC_ID |
| kakaoUrlPath | String | N | 카카오맵 업체 상세 이동 | PLC.KAKAO_URL_PATH, HOS-001-26 |

### PlaceSummary
쓰는 API: GET /places
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| plcSn | Long | Y | 초안 그대로 | PLC.PLC_SN |
| plcClsfCd | String | Y | 초안 그대로. 공통코드 PLCG01 | PLC.PLC_CLSF_CD |
| plcClsfCdNm | String | Y | 업체 유형 표시 | COM_CD.COM_CD_NM, HOS-001-07 |
| plcConm | String | Y | 초안 그대로 | PLC.PLC_CONM |
| plcAddr | String | Y | 초안 그대로 | PLC.PLC_ADDR |
| rprsTelno | String | N | 초안 그대로 | PLC.RPRS_TELNO |
| plcLat | BigDecimal | Y | 초안 그대로 | PLC.PLC_LAT |
| plcLot | BigDecimal | Y | 초안 그대로 | PLC.PLC_LOT |
| kakaoPlcId | String | N | 초안 그대로 | PLC.KAKAO_PLC_ID |
| kakaoUrlPath | String | N | 초안 그대로 | PLC.KAKAO_URL_PATH |
| distance | Integer | N | 초안 그대로. 마지막 검색 실행 시점의 지도 중앙 기준. 단위는 명세 없음(issues 15) | HOS-001-07 |
| plcBkmkSn | Long | N | 초안 그대로. 내가 즐겨찾기한 업체면 값 | PLC_BKMK.PLC_BKMK_SN (HOS-001-20 즐겨찾기 핀) |
