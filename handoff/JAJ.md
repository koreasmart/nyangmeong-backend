# JAJ 핸드오프

## 지금 하는 것

끝남: SCR-HLT-003 케어 캘린더 (일정) — 일정 조회·등록·수정·삭제 API(FN-HLT-003-29~37·40, FN-AIM-012-13·14). 검증 통과(3회차), 사람 확인 완료 (2026-10-26). 추가-1 프론트 연결은 프론트 저장소로 넘김

## 다음에 할 것

- 연결 남음: 추가-1 (프론트 연결 `features/health/api.js` 등 일정 화면, 프론트 저장소에서 /start — 마일스톤 "SCR-HLT-003 케어 캘린더 · 일정 API 연결")
- 연결 남음: FN-HLT-003-39 (반려동물 목록 API, SJM)
- 연결 남음: FN-AIM-012-13·14 (AI 대화 일정 저장 화면 SCR-AIM-012, LJS)
- 참고: 기획의 `SCHEDULE_NOT_FOUND`는 기존 `CARE_SCHEDULE_NOT_FOUND`를 그대로 썼다(ErrorCode 추가 없음)
- HLTG21(캘린더 뱃지 색상) 하위 코드가 `db/` 공통코드에 없다. 코드 값을 정해 `db/migration`에 넣는 것을 팀과 맞춘다(그 전엔 색상 저장 불가)
- 통합 캘린더 호출 방식(issues 공통레이아웃·메인 8번): 지금은 반려동물별 호출. 다건 조회 API가 필요하면 다시 정한다
- LJS `CareScheduleAlarmService`(AIM-006)·`CheckupRecordFake`(AIM-009)를 health service로 연결하는 것은 LJS와 맞춘다
- `application.properties`에 빠진 키: `app.front-base-url`, `app.seed.password` (디스코드의 최신 파일로 받는다)
