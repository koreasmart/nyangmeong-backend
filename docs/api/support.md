# 고객센터(support) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /faqs | FAQ 목록(카테고리·키워드, 답변 포함, 10건) | CSC-002-01~03·05 | KSJ | - | Page<FaqSummary> |
| GET | /inquiries | 내 1:1 문의 목록(처리 상태 필터) | CSC-003-01~04·16·17 | KSJ | - | Page<InquirySummary> |
| GET | /inquiries/{otoInqrySn} | 내 1:1 문의 상세(답변 포함) | CSC-003-12·13·16 | KSJ | - | InquiryResponse |
| GET | /notices | 공지사항 목록(상단 고정 우선·제목 검색, 10건). 상세 하단 최신 5건도 사용 | CSC-001-01~04·06 | KSJ | - | Page<NoticeSummary> |
| GET | /notices/{ntcMttrSn} | 공지사항 상세(조회수 세션 기준 1회 증가) | CSC-001-05·07 | KSJ | - | NoticeResponse |
| POST | /inquiries | 1:1 문의 작성(multipart, 캐릭터 오류 문의 연동 포함, 접수 상태) | CSC-003-06·08~11, MYP-010-12·24 (기능 분배 3.1절 21·24번) | KSJ | InquiryCreateRequest | InquiryResponse |
| PATCH | /inquiries/{otoInqrySn} | 1:1 문의 수정(multipart, 접수 상태에서만) | CSC-003-15 | KSJ | InquiryUpdateRequest | InquiryResponse |

## DTO

### FaqSummary
쓰는 API: GET /faqs
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| faqSn | Long | Y | | FAQ.FAQ_SN |
| faqClsfCd | String | Y | 공통코드 CSCG01 | FAQ.FAQ_CLSF_CD, CSC-002-02 |
| faqClsfCdNm | String | Y | 카테고리 배지 | COM_CD.COM_CD_NM, CSC-002-01 |
| qstnTtl | String | Y | | FAQ.QSTN_TTL, CSC-002-01 |
| ansCn | String | Y | 아코디언 답변 | FAQ.ANS_CN, CSC-002-04 |

### InquiryCreateRequest
쓰는 API: POST /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inqryTypeCd | String | Y | 초안 그대로. 공통코드 CSCG02 단일 선택 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-06 |
| inqryTtl | String | Y | 최대 50자, 공백만은 불가(명세 v0.4, 2026-10-26 초안의 200자에서 변경) | OTO_INQRY.INQRY_TTL, CSC-003-08 |
| inqryCn | String | Y | 최대 2000자, 공백만은 불가(명세 v0.4, 2026-10-26 초안의 4000자에서 변경) | OTO_INQRY.INQRY_CN, CSC-003-08 |
| images | List<MultipartFile> | N | multipart/form-data로 함께 보낸다(2026-10-26 `atchFileSns`에서 변경: 파일을 먼저 올리는 공용 업로드 API가 없다). 최대 10개, 각 10MB 이내, JPG·PNG·WEBP | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-09 |
| pixelCharSn | Long | N | multipart. 캐릭터 오류 문의면 보낸다. 유형이 픽셀 캐릭터여야 하고, 저장 뒤 pet의 오류 접수가 실패하면(이미 접수·생성 중 등) 문의도 저장되지 않는다. 접수가 돌려준 캐릭터 이미지는 첨부 첫 번째로 붙고 첨부 합계 10개에 포함된다(사용자 이미지는 최대 9개) | OTO_INQRY.PIXEL_CHAR_SN (캐릭터 오류 문의), CSC-003-10 |
| charPhotoSn | Long | N | multipart. 초안 그대로. `pixelCharSn`과 함께 보낼 때만 받는다 | OTO_INQRY.CHAR_PHOTO_SN (캐릭터 오류 문의), CSC-003-10 |

### InquiryResponse
쓰는 API: GET /inquiries/{otoInqrySn}; PATCH /inquiries/{otoInqrySn}; POST /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | 초안 그대로 | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | 초안 그대로. 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD |
| inqryTypeCdNm | String | Y | 유형 표시 | COM_CD.COM_CD_NM, CSC-003-12 |
| inqryPrcsSttsCd | String | Y | 초안 그대로. 공통코드 CSCG03 | OTO_INQRY.INQRY_PRCS_STTS_CD |
| inqryPrcsSttsCdNm | String | Y | 상태 표시 | COM_CD.COM_CD_NM, CSC-003-12 |
| inqryTtl | String | Y | 초안 그대로 | OTO_INQRY.INQRY_TTL |
| inqryCn | String | Y | | OTO_INQRY.INQRY_CN, CSC-003-12 |
| imgs | List<ImageItem> | Y | 없으면 빈 목록 | INQRY_ATCH_PHOTO, CSC-003-12 |
| regDt | LocalDateTime | Y | 접수일 | 공통 감사 컬럼 REG_DT, CSC-003-12 |
| ansCn | String | N | 답변 전 null | OTO_INQRY.ANS_CN, CSC-003-13 |
| ansDt | LocalDateTime | N | 답변 전 null | OTO_INQRY.ANS_DT, CSC-003-13 |

### InquirySummary
쓰는 API: GET /inquiries
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| otoInqrySn | Long | Y | | OTO_INQRY.OTO_INQRY_SN |
| inqryTypeCd | String | Y | 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-01 |
| inqryTypeCdNm | String | Y | 유형 표시 | COM_CD.COM_CD_NM, CSC-003-01 |
| inqryTtl | String | Y | | OTO_INQRY.INQRY_TTL, CSC-003-01 |
| inqryPrcsSttsCd | String | Y | 공통코드 CSCG03(접수·답변 완료) | OTO_INQRY.INQRY_PRCS_STTS_CD, CSC-003-01·02 |
| inqryPrcsSttsCdNm | String | Y | 상태 표시 | COM_CD.COM_CD_NM, CSC-003-01 |
| regDt | LocalDateTime | Y | 접수일 | 공통 감사 컬럼 REG_DT, CSC-003-01 |

### InquiryUpdateRequest
쓰는 API: PATCH /inquiries/{otoInqrySn}
접수 상태에서만 수정할 수 있다(CSC-003-15). 수정 가능한 범위는 issues 13.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inqryTypeCd | String | N | 공통코드 CSCG02 | OTO_INQRY.INQRY_TYPE_CD, CSC-003-06 |
| inqryTtl | String | N | 최대 50자(2026-10-26 200자에서 변경) | OTO_INQRY.INQRY_TTL, CSC-003-08·15 |
| inqryCn | String | N | 최대 2000자(2026-10-26 4000자에서 변경) | OTO_INQRY.INQRY_CN, CSC-003-08·15 |
| keepAtchFileSns | List<Long> | N | multipart. 남길 기존 첨부. 보낸 경우 여기에 없는 기존 첨부는 삭제한다(2026-10-26 `atchFileSns` 목록 교체에서 변경). 보내지 않으면 기존 첨부를 그대로 둔다 | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-15 |
| images | List<MultipartFile> | N | multipart. 새로 추가할 첨부. 남는 첨부와 합쳐 최대 10개, 각 10MB 이내 | INQRY_ATCH_PHOTO.ATCH_FILE_SN, CSC-003-09 |

### NoticeResponse
쓰는 API: GET /notices/{ntcMttrSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL, CSC-001-05 |
| ntcMttrCn | String | Y | 본문(텍스트 및 이미지, 이미지 담는 방식 issues 10) | NTC_MTTR.NTC_MTTR_CN, CSC-001-05 |
| inqCnt | Integer | Y | 세션 기준 1회 증가 반영 후 값 | NTC_MTTR.INQ_CNT, CSC-001-05·07 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CSC-001-05 |

### NoticeSummary
쓰는 API: GET /notices
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ntcMttrSn | Long | Y | | NTC_MTTR.NTC_MTTR_SN |
| ntcMttrTtl | String | Y | | NTC_MTTR.NTC_MTTR_TTL, CSC-001-01 |
| upendFixYn | boolean | Y | 상단 고정 배경색 구분 | NTC_MTTR.UPEND_FIX_YN, CSC-001-02 |
| regDt | LocalDateTime | Y | 작성일 | 공통 감사 컬럼 REG_DT, CSC-001-01·06 |
