# 회원(member) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /auth/login-attempt-limits | 입력 아이디 기준 실패 횟수·캡챠 요구 상태 조회(`?email=`, 가입 여부 비공개) | ACC-002-03·22 | YAY | - | LoginAttemptLimitResponse |
| GET | /email-certifications/{emlCertSn} | 인증·재설정 권한의 유효(만료·사용 완료) 상태 조회 | ACC-012-06 | YAY | - | EmailCertificationResponse |
| GET | /members/me | 회원정보 수정 화면 조회. 이메일·닉네임·휴대전화·프로필 사진·가입 경로·연동 SNS | MYP-040-02·03·08·09 | SJM | - | MemberResponse |
| GET | /members/me/dashboard-widgets | 메인 건강 지표 위젯의 표시 여부·순서 조회 | MAN-006·007 | YAY | - | List<DashboardWidgetSummary> |
| GET | /members/me/recovery | 복구 대상 이메일·닉네임·남은 복구 기간·필요 동의·보관 번호 조회(복구 확인 권한) | ACC-001-44 | YAY | - | MemberRecoveryResponse |
| GET | /auth/me | 로그인한 회원 요약(회원 번호·권한·닉네임·프로필 사진·마스킹 이메일). 구현은 `auth` 도메인(nct-backend `nct.auth`와 같은 자리) | COM-002-15, COM-005-02 | LJS | - | AuthMeResponse |
| GET | /internal/auth/check | 서버 간 내부 인증 확인(Python 미들웨어가 `/ai/**` 요청마다 사용자 쿠키를 넘겨 호출). `/api` 접두어 없음, 서버 간 키 없음(사용자 인증만). 세션 폐기 401 `UNAUTHORIZED`, 정지·탈퇴 403 `MEMBER_INACTIVE`(쿠키 삭제 `Set-Cookie`) | - (2026-10-26 사람 결정) | LJS | - | InternalAuthCheckResponse |
| GET | /members/nickname-availability | 닉네임 형식(한글·영문·숫자 1~10자)·중복 확인 | ACC-001-08, MYP-040-04·10 (기능 분배 3.1절 2번) | YAY | - | MemberNicknameAvailabilityResponse |
| GET | /members/phone-availability | 휴대전화번호 정규화(+82·하이픈)·형식·중복 확인 | ACC-001-29·30·44, MYP-040-05·11 (기능 분배 3.1절 3번) | YAY | - | MemberPhoneAvailabilityResponse |
| GET | /terms | 현재 사용 중인 약관 목록(필수 여부·버전) | ACC-001-12·44 | YAY | - | List<TermsSummary> |
| GET | /terms/{trmsSn} | 약관 전문 조회 | ACC-001-13 | YAY | - | TermsResponse |
| POST | /auth/captchas | 보안코드(캡챠) 발급·새로고침, 이전 코드 무효화 | ACC-002-18·19 | YAY | CaptchaCreateRequest | CaptchaResponse |
| POST | /auth/login | 이메일 로그인(캡챠·ID/PW 검증, 실패 누적, 첫 로그인 닉네임 재안내 여부 반환). 실패는 4xx 오류 응답이고 `data`에 `LoginAttemptLimitResponse`(실패 횟수·보안코드 요구)가 실린다. 코드는 LOGIN_FAILED(400, 없는 아이디·SNS 전용도 같음), LOGIN_CAPTCHA_REQUIRED(429, 보안코드 없음·5회째 실패), CAPTCHA_MISMATCH(400), MEMBER_WITHDRAWN(403, 비밀번호가 맞은 탈퇴 회원), MEMBER_INACTIVE(403, 정지) | ACC-002-05·16·20, ACC-001-22 | YAY | LoginRequest | LoginResponse |
| POST | /auth/logout | 로그아웃(Refresh 토큰 폐기) | COM-005-07·08, AIM-011-17 (기능 분배 3.1절 28번) | YAY | - | - |
| POST | /auth/refresh | Access 토큰 재발급. 토큰은 httpOnly 쿠키로만 주고 응답 `data`는 없다(`docs/convention/api.md`). 근거는 로그인 유지 기능(issues 6) | (명세 기능 ID 없음) | YAY | - | - |
| POST | /auth/sns-login | 네이버·카카오·구글 인가 결과 처리: 기존 회원 로그인 / 신규 가입 확인 / 이메일 중복 / 30일 복구 분기 | ACC-002-09~11, ACC-001-22·31·38·40 | YAY | SnsLoginRequest | SnsLoginResponse |
| POST | /email-certifications | 이메일 인증번호 발송·재전송(목적: 가입·비밀번호 찾기·복구). 가입 목적은 정상 회원 이메일 중복이면 거절(409 EMAIL_ALREADY_REGISTERED, 탈퇴 보관 이메일은 409 MEMBER_RECOVERY_AVAILABLE). 재전송 간격·횟수에 걸리면 400 EMAIL_CERT_RESEND_NOT_YET이고 data의 `resendAvailableDt`가 재시도 가능 시각. 지금은 가입 목적(USRC0010)만 구현 | ACC-001-03·04·38·41, ACC-011-03·07 | YAY | EmailCertificationCreateRequest | EmailCertificationResponse |
| POST | /email-certifications/{emlCertSn}/invalidate | 인증번호·재설정 권한·복구 확인 권한 무효화 | ACC-011-05, ACC-012-04, ACC-001-46 | YAY | - | - |
| POST | /email-certifications/{emlCertSn}/password-reset | 본인확인 결과로 비밀번호 재설정, 대상 계정 기존 로그인 종료 | ACC-012-01~03 | YAY | EmailCertificationPasswordResetRequest | - |
| POST | /email-certifications/{emlCertSn}/verify | 인증번호 확인. 목적별 결과(가입 진행 / 재설정 대상·SNS 전용·미가입·탈퇴 보관 분기 / 복구 확인 권한) 반환 | ACC-001-06·40·43, ACC-011-08~10 | YAY | EmailCertificationVerifyRequest | EmailCertificationResponse |
| POST | /members | 회원가입(이메일·SNS 공통). 약관별 동의·번호 암호문/HMAC·기본 닉네임·최초 상태 이력·SNS 연결 저장, 서버 재검사. 201과 함께 닉네임 설정 전용 쿠키 `ncnm_setup`(로그인 쿠키 아님)을 준다. 이메일 가입만 구현(SNS 가입은 SNS 로그인 마일스톤) | ACC-001-09·10·12·14·22·24·27·29·32·38 | YAY | MemberCreateRequest | MemberResponse |
| POST | /members/me/password-verification | 현재 비밀번호 확인(회원정보 수정 진입·비밀번호 변경 전) *통합 때 추가* | MYP-040-01 | YAY | PasswordVerifyRequest | - |
| POST | /members/me/recover | 30일 이내 탈퇴 회원 기존 정보 복구 실행(탈퇴 → 정상 이력) | ACC-001-45 | YAY | MemberRecoverRequest | - |
| POST | /members/me/withdraw | 회원탈퇴(상태·이력 반영, 모든 로그인·인증 권한 종료, 30일 보관) | ACC-020-03·14·16, AIM-011-19, MYP-002-08 (기능 분배 3.1절 5번) | YAY | MemberWithdrawRequest | - |
| PUT | /members/me/dashboard-widgets | 메인 건강 지표 위젯 표시 여부·순서 전체 저장 | MAN-007 | YAY | DashboardWidgetSaveRequest | List<DashboardWidgetSummary> |
| PATCH | /members/me | 회원정보 수정. 휴대전화(저장 시 중복 재검사)·프로필 사진 | MYP-040-02·05·08·11 | SJM | MemberUpdateRequest | MemberResponse |
| PATCH | /members/me/nickname | 닉네임 설정 확정(로그인 회원 또는 가입 직후 받은 `ncnm_setup` 쿠키, 둘 다 없으면 401), 설정 완료 여부 반영 | ACC-001-26 | YAY | MemberNicknameUpdateRequest | MemberResponse |
| PATCH | /members/me/password | 로그인 상태에서 비밀번호 변경 *통합 때 추가* | MYP-040-06 | YAY | PasswordChangeRequest | - |

## DTO

### CaptchaCreateRequest
쓰는 API: POST /auth/captchas
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 이메일 형식. 이 아이디의 보안코드를 새로 발급하고 이전 코드는 무효화한다. 가입 여부와 무관하게 같은 처리. 보안코드를 요구하는 상태(5회 실패)가 아닌 아이디는 CAPTCHA_NOT_REQUIRED(400)로 거절한다 | LGN_ATMPT_LMT.LGN_ID_HMAC (HMAC 전 원문), ACC-002-19 |

### CaptchaResponse
쓰는 API: POST /auth/captchas
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| captchaImg | String | Y | 영문 대문자·숫자 각 1개 이상, 총 6글자를 그린 이미지. PNG를 Base64 data URI(`data:image/png;base64,...`)로 준다. 코드 원문은 주지 않는다 | ACC-002-18 |

### DashboardWidgetSaveItem
쓰는 API: PUT /members/me/dashboard-widgets (DashboardWidgetSaveRequest.widgets의 요소. 입력 목록 밖의 중첩 record, issues 2)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dshbdWdgtCd | String | Y | MANG01 공통코드, 8자 | MBR_DSHBD_WDGT.DSHBD_WDGT_CD |
| indctYn | Boolean | Y | | MBR_DSHBD_WDGT.INDCT_YN, MAN-006 |
| sortSeq | Integer | Y | 최대 10자리 | MBR_DSHBD_WDGT.SORT_SEQ, MAN-007 |

### DashboardWidgetSaveRequest
쓰는 API: PUT /members/me/dashboard-widgets
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| widgets | List<DashboardWidgetSaveItem> | Y | 위젯 전체 목록(PUT, 빠진 항목은 지워짐). `indctYn=true`는 최대 5개 | MAN-006·007 |

### DashboardWidgetSummary
쓰는 API: GET /members/me/dashboard-widgets; PUT /members/me/dashboard-widgets (응답 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dshbdWdgtCd | String | Y | MANG01 공통코드 | MBR_DSHBD_WDGT.DSHBD_WDGT_CD |
| dshbdWdgtCdNm | String | Y | 위젯 카드 이름(체중·식사량 등) | COM_CD.COM_CD_NM, MAN-006 (추가) |
| indctYn | boolean | Y | | MBR_DSHBD_WDGT.INDCT_YN, MAN-006 |
| sortSeq | Integer | Y | | MBR_DSHBD_WDGT.SORT_SEQ, MAN-007 |

### EmailCertificationCreateRequest
쓰는 API: POST /email-certifications
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertPrpsCd | String | Y | 공통코드 USRG04(가입·비밀번호 찾기·복구) | EML_CERT.EML_CERT_PRPS_CD |
| certTrgtEml | String | Y | 이메일 형식. 가입 목적이면 정상 회원 이메일 중복 시 거절 | EML_CERT.CERT_TRGT_EML_ENC, ACC-001-03, ACC-011-02 |

### EmailCertificationPasswordResetRequest
쓰는 API: POST /email-certifications/{emlCertSn}/password-reset
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pswdInitTkn | String | Y | 인증 확인 때 받은 재설정 권한 토큰. 만료·사용 완료면 거절 | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-012-06 |
| pswd | String | Y | 8자 이상, 문자와 숫자 조합 | MBR.PSWD_HASH (해시 전 원문), ACC-012-01 |
| pswdCnfrm | String | Y | `pswd`와 일치. 서버가 다시 검사 | ACC-012-02·03 |

### EmailCertificationResponse
쓰는 API: GET /email-certifications/{emlCertSn}; POST /email-certifications; POST /email-certifications/{emlCertSn}/verify
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertSn | Long | Y | | EML_CERT.EML_CERT_SN |
| emlCertPrpsCd | String | Y | 공통코드 USRG04 | EML_CERT.EML_CERT_PRPS_CD |
| certExpryDt | LocalDateTime | Y | 남은 시간 표시 기준(서버 만료시각) | EML_CERT.CERT_EXPRY_DT, ACC-001-05 |
| resendAvailableDt | LocalDateTime | N | 재전송 제한 중이면 재시도 가능 시점 | ACC-011-07 |
| certCmptnDt | LocalDateTime | N | 인증 확인 성공 시각. verify 이후 | EML_CERT.CERT_CMPTN_DT |
| certRslt | String | N | verify 결과 분기. 가입 목적은 `SIGNUP_READY`로 확정. 복구 대상 / 재설정 대상 / SNS 전용 / 미가입 / 탈퇴 보관은 해당 마일스톤에서 값 확정 | ACC-001-06·43, ACC-011-08 |
| snsOffrCds | List<String> | N | `certRslt`가 SNS 전용일 때 실제 연결된 제공자(USRG03) | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-011-10 |
| pswdInitTkn | String | N | 재설정 대상일 때만. 비밀번호 재설정 권한 토큰 원문 | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-011-08 |
| initAuthrtExpryDt | LocalDateTime | N | 재설정 권한 만료 시각 | EML_CERT.INIT_AUTHRT_EXPRY_DT |
| resetAvailable | Boolean | N | GET 조회 시 재설정 권한이 유효한지(만료·사용 완료·무효화면 false) | EML_CERT.CERT_USE_CMPTN_DT, EML_CERT.CERT_INVLD_DT, ACC-012-06 |
| rcvryAuthrtTkn | String | N | 복구 대상일 때만. 복구 확인 권한 토큰(저장 컬럼 없음, issues 5) | ACC-001-43 |

### EmailCertificationVerifyRequest
쓰는 API: POST /email-certifications/{emlCertSn}/verify
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| certNo | String | Y | 숫자 6자리. 다른 목적·이미 사용한 번호는 거절 | EML_CERT.CERT_NO_HASH (해시 전 원문), ACC-001-05, ACC-001-42 |

### LoginAttemptLimitResponse
쓰는 API: GET /auth/login-attempt-limits
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| scrtyCdRqtYn | boolean | Y | 보안코드 입력이 필요한지. 기록 없는 아이디는 false | LGN_ATMPT_LMT.SCRTY_CD_RQT_YN, ACC-002-22 |
| cntnuCertFailNmtm | Integer | Y | 연속 인증 실패 횟수. 기록 없는 아이디는 0(가입 여부 비공개) | LGN_ATMPT_LMT.CNTNU_CERT_FAIL_NMTM, ACC-002-03 |

### LoginRequest
쓰는 API: POST /auth/login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 미입력 불가, 이메일 형식 | MBR.ACNT_EML_ENC, ACC-002-03 |
| pswd | String | Y | 미입력 불가 | MBR.PSWD_HASH (해시 전 원문), ACC-002-04 |
| scrtyCd | String | N | 보안코드 요구 상태면 필수. 영문·숫자 6글자, 소문자는 대문자로 처리 | LGN_ATMPT_LMT.SCRTY_CD_HASH (해시 전 원문), ACC-002-20 |

### LoginResponse
쓰는 API: POST /auth/login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ncnmReguide | Boolean | Y | 닉네임 모달 재안내 여부. 닉네임 설정 미완료이고 기존 최종로그인일시가 없을 때만 true | MBR.NCNM_STNG_CMPTN_YN, MBR.LAST_LGN_DT, ACC-001-22 |
| mbrNcnm | String | Y | 재안내 모달에 미리 채울 저장값 | MBR.MBR_NCNM, ACC-001-22 |

### MemberCreateRequest
쓰는 API: POST /members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| joinPathCd | String | Y | 공통코드 USRG03(이메일·네이버·카카오·구글) | MBR.JOIN_PATH_CD |
| emlCertSn | Long | N | 이메일 가입이면 필수. 가입 목적 인증이 완료·유효해야 한다. 이메일은 이 인증의 대상 이메일을 쓴다 | EML_CERT.EML_CERT_SN, ACC-001-14 |
| certNo | String | N | 이메일 가입이면 필수. 숫자 6자리 인증번호를 한 번 더 확인한다. `emlCertSn`이 순번이라 남이 인증해 둔 번호로 가입하는 것을 막는다(issues 로그인 6) | EML_CERT.CERT_NO_HASH, ACC-001-14 |
| signupTkn | String | N | 지금은 받지 않는다(SNS 가입 마일스톤에서 추가). SNS 가입이면 필수. `POST /auth/sns-login`이 준 신규 가입 확인 토큰(저장소 미정, issues 5) | ACC-001-31·32 |
| mblTelno | String | Y | 공백·하이픈 제거, +82 표기 통일 후 형식 검사. 다른 회원(30일 보관 포함)과 중복 불가 | MBR.MBL_TELNO_ENC, MBR.MBL_TELNO_HMAC, ACC-001-29·30 |
| pswd | String | N | 이메일 가입이면 필수, SNS 가입이면 보내지 않는다. 8자 이상 64자 이하, 영문과 숫자를 함께 포함하는 공백 없는 ASCII(BCrypt 72바이트 한계) | MBR.PSWD_HASH (해시 전 원문), ACC-001-09, ACC-001-32 |
| pswdCnfrm | String | N | 이메일 가입이면 필수. `pswd`와 일치 | ACC-001-10 |
| trmsAgres | List<TermsAgreeRequest> | Y | 화면에 보여 준 약관 전부. 필수 약관(서비스 이용약관·개인정보 수집 및 이용·만 14세 이상) 모두 `agreYn=true`. 마케팅 미동의도 false로 보낸다 | MBR_TRMS_AGRE, ACC-001-12 |
| idmptDmndId | String | N | 같은 가입 시도의 재요청 식별값. 저장 컬럼이 없어 받기만 한다. 중복 클릭·응답 유실 재요청은 같은 인증(`emlCertSn`·`certNo`)의 사용 기록으로 가려 닉네임 쿠키 유효시간 안에서 기존 결과만 돌려준다 | ACC-001-14, ACC-001-32 |

#### TermsAgreeRequest (요청 record, 쓰는 DTO: MemberCreateRequest, MemberRecoverRequest)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | 현재 사용 중인 약관 버전이어야 한다 | MBR_TRMS_AGRE.TRMS_SN, ACC-001-12 |
| agreYn | Boolean | Y | | MBR_TRMS_AGRE.AGRE_YN |

### MemberNicknameAvailabilityResponse
쓰는 API: GET /members/nickname-availability
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| available | Boolean | Y | 초안 그대로. 현재 본인 닉네임이면 true | ACC-001-08 |

### MemberNicknameUpdateRequest
쓰는 API: PATCH /members/me/nickname
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrNcnm | String | N | 한글·영문·숫자 1~10자, 공백·특수문자·이모지 불가, 중복 불가. 저장 시 설정 완료 여부도 Y로 바꾼다. 기본 닉네임을 그대로 확정할 때도 현재 값을 보낸다(issues 3) | MBR.MBR_NCNM, MBR.NCNM_STNG_CMPTN_YN, ACC-001-07·26 |

### MemberPhoneAvailabilityResponse
쓰는 API: GET /members/phone-availability
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| normalizedMblTelno | String | Y | 공백·하이픈 제거, +82 표기를 국내 표기 숫자(`01012345678`)로 통일한 결과 | ACC-001-29 |
| available | Boolean | Y | 초안 그대로 | ACC-001-30 |

### MemberRecoverRequest
쓰는 API: POST /members/me/recover
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsAgres | List<TermsAgreeRequest> | N | 같은 버전으로 이미 동의한 약관은 빼고, 빠진 현재 버전 필수 약관만. 빠진 필수 약관이 있으면 필수 | MBR_TRMS_AGRE, ACC-001-44·45 |
| mblTelno | String | N | 보관 번호가 없을 때만 필수. 정규화·형식·중복 검사는 가입과 같다 | MBR.MBL_TELNO_ENC, ACC-001-29, ACC-001-44 |
| idmptDmndId | String | N | 중복 클릭·응답 유실 시 같은 요청의 결과를 확인하는 식별값(저장 컬럼 없음, issues 5) | ACC-001-45 |

### MemberRecoveryResponse
쓰는 API: GET /members/me/recovery
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 본인확인을 마친 복구 대상 이메일 | MBR.ACNT_EML_ENC, ACC-001-44 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ACC-001-44 |
| whdwlDt | LocalDateTime | Y | | MBR.WHDWL_DT, ACC-001-44 |
| rcvryExpryDt | LocalDateTime | Y | 보관 만료시각 = 탈퇴일시 + 탈퇴회원보관일수. 남은 기간 표시 기준 | MBR.WHDWL_DT, SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT, ACC-001-44 |
| maskedMblTelno | String | N | 보관 중인 번호를 가려서. 없으면 null(직접 입력) | MBR.MBL_TELNO_ENC, ACC-001-44, ACC-020-01 |
| missingTrms | List<TermsSummary> | Y | 다시 받아야 할 현재 버전 약관. 없으면 빈 목록 | TRMS, MBR_TRMS_AGRE, ACC-001-44 |

### MemberResponse
쓰는 API: GET /members/me; PATCH /members/me; PATCH /members/me/nickname; POST /members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 복호화한 로그인 이메일(수정 불가 표시) | MBR.ACNT_EML_ENC, MYP-040-03 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, MYP-040-04, ACC-001-24·27 |
| mblTelno | String | Y | 복호화한 휴대전화번호 | MBR.MBL_TELNO_ENC, MYP-040-05 |
| atchFileSn | Long | N | 프로필 사진. null이면 기본 이미지 | MBR.ATCH_FILE_SN, MYP-040-02 |
| joinPathCd | String | Y | 가입 경로(USRG03). SNS 회원은 본인 확인 생략 | MBR.JOIN_PATH_CD, MYP-040-01 |
| ncnmStngCmptnYn | boolean | Y | 닉네임 설정 완료 여부 | MBR.NCNM_STNG_CMPTN_YN, ACC-001-26·27 |
| snsAcntLinks | List<MemberSnsAccountSummary> | Y | 연동된 SNS 계정. 없으면 빈 목록 | SNS_ACNT_LINK, MYP-040-09 |

#### MemberSnsAccountSummary (MemberResponse.snsAcntLinks)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsOffrCd | String | Y | SNS 제공자(USRG03) | SNS_ACNT_LINK.SNS_OFFR_CD, MYP-040-09 |

### AuthMeResponse
쓰는 API: GET /auth/me
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 로그인한 회원 번호 | MBR.MBR_SN |
| mbrAuthrtCd | String | Y | USRG02 공통코드. 관리자 메뉴 표시용 | MBR.MBR_AUTHRT_CD |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| atchFileSn | Long | N | 미등록이면 응답에서 빠짐 → 기본 이미지 | MBR.ATCH_FILE_SN, COM-002-15 |
| maskedAcntEml | String | Y | 마스킹한 계정 이메일(`jin****@gmail.com`, @ 앞 3자 이하면 첫 글자만) | MBR.ACNT_EML_ENC, COM-005-02 |

### InternalAuthCheckResponse
쓰는 API: GET /internal/auth/check
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 쿠키의 회원 번호(인증 필터가 판정) | MBR.MBR_SN |
| mbrAuthrtCd | String | Y | USRG02 공통코드 | MBR.MBR_AUTHRT_CD |

### MemberUpdateRequest
쓰는 API: PATCH /members/me
`multipart/form-data`(issues 5번)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mblTelno | String | N | 공백·하이픈 제거, +82를 국내 표기로 통일한 뒤 형식 검사. 다른 회원과 중복 불가(저장 시 서버 재검사) | MBR.MBL_TELNO_ENC, MYP-040-05·11 |
| profilePhoto | MultipartFile | N | 새 프로필 사진. 형식·용량 규칙은 명세에 없음 | MBR.ATCH_FILE_SN, MYP-040-02·08 |
| profilePhotoReset | Boolean | N | `true`면 기본 이미지로 되돌림(`profilePhoto`와 함께 보내면 400) | MBR.ATCH_FILE_SN, MYP-040-02 |

### MemberWithdrawRequest
쓰는 API: POST /members/me/withdraw
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| noticeConfirmed | Boolean | Y | 초안 그대로. true여야 실행 | ACC-020-03 |
| confirmPhrase | String | Y | 초안 그대로. "정말로 탈퇴하겠습니다."와 완전 일치(띄어쓰기·마침표·앞뒤 공백 포함). 원문을 저장하지 않는다 | ACC-020-20, ACC-020-14 |

### PasswordChangeRequest
쓰는 API: PATCH /members/me/password
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pswd | String | N | 새 비밀번호. 8자 이상, 문자와 숫자 조합(가입·재설정 규칙 준용). PATCH 규칙상 N이지만 실제로는 필수(issues 3·4) | MBR.PSWD_HASH (해시 전 원문), MYP-040-06, ACC-001-09, ACC-012-01 |
| pswdCnfrm | String | N | `pswd`와 일치(재설정 규칙 준용) | MYP-040-06, ACC-012-02 |

### PasswordVerifyRequest
쓰는 API: POST /members/me/password-verification
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pswd | String | Y | 현재 비밀번호. 미입력 불가(로그인 비밀번호 미입력 규칙 준용). 형식 규칙은 적용하지 않는다 | MBR.PSWD_HASH (해시 전 원문), MYP-040-01, ACC-002-04 |

### SnsLoginRequest
쓰는 API: POST /auth/sns-login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsOffrCd | String | Y | 공통코드 USRG03(네이버·카카오·구글) | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-002-09~11 |
| code | String | Y | 제공자 인가 결과 코드. 흐름은 `issues.md` API의 로그인·플레이스·커뮤니티·고객센터 2번에서 미정 | ACC-002-09~11 |

### SnsLoginResponse
쓰는 API: POST /auth/sns-login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsLoginRslt | String | Y | 기존 회원 로그인 / 신규 가입 확인 / 이메일 중복 / 30일 복구 중 하나(값 미정) | ACC-002-09~11 |
| ncnmReguide | Boolean | N | 기존 회원 로그인일 때만. `LoginResponse`와 같은 기준 | MBR.NCNM_STNG_CMPTN_YN, MBR.LAST_LGN_DT, ACC-001-22 |
| mbrNcnm | String | N | 기존 회원 로그인일 때만. 재안내 모달 저장값 | MBR.MBR_NCNM, ACC-001-22 |
| snsOffrCd | String | N | 신규 가입 확인일 때 읽기 전용 표시 | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-001-31 |
| acntEml | String | N | 신규 가입 확인·복구일 때 읽기 전용 표시(제공자가 준 이메일) | ACC-001-31, ACC-001-40 |
| mblTelno | String | N | 신규 가입 확인일 때 제공자가 준 번호. 미제공이면 null(직접 입력) | MBR.MBL_TELNO_ENC, ACC-001-29 |
| ageCnfrmd | Boolean | N | 신규 가입 확인일 때 외부 연령 확인이 검증됐는지. true면 "확인 완료" 표시 | ACC-001-12, ACC-001-31 |
| signupTkn | String | N | 신규 가입 확인일 때 `POST /members`로 넘길 확인 토큰(저장소 미정, issues 5) | ACC-001-31 |
| rcvryAuthrtTkn | String | N | 30일 복구이고 기존 연결과 같은 SNS 계정으로 인증했을 때만. 미연결 SNS면 null(이메일 추가 인증) | ACC-002-09, ACC-001-40 |

### TermsResponse
쓰는 API: GET /terms/{trmsSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | | TRMS.TRMS_SN |
| trmsFixIdntfKey | String | Y | | TRMS.TRMS_FIX_IDNTF_KEY |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID, ACC-001-13 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL |
| trmsCn | String | Y | 전문. 길이 때문에 자르지 않는다 | TRMS.TRMS_CN, ACC-001-13 |
| esntlAgreYn | boolean | Y | | TRMS.ESNTL_AGRE_YN |

### TermsSummary
쓰는 API: GET /terms (`MemberRecoveryResponse.missingTrms`에서도 씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | | TRMS.TRMS_SN |
| trmsFixIdntfKey | String | Y | 항목 구분. 내용보기 없는 연령 확인 항목 식별에도 쓴다 | TRMS.TRMS_FIX_IDNTF_KEY, ACC-001-13 |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID, ACC-001-12 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL |
| esntlAgreYn | boolean | Y | | TRMS.ESNTL_AGRE_YN, ACC-001-12 |
| sortSeq | Integer | Y | | TRMS.SORT_SEQ |
