# SJM 핸드오프

## 지금 하는 것
SCR-MYP-020 포인트 관리/케어미션 — 진행 중: 구현·검증 끝(판정 확인 못 함 있음, 연결 남음만), 3-1·추가-1 사람 확인은 병합 뒤 (승인 2026-10-27 06:14, 시작 2026-10-26 16:58)
- 기준: 마이페이지_기능명세서_v0.4_20261025, 마이페이지_화면설계서_v0.6_20261025, 건강기록_기능명세서_v1.1_20261026(FN-HLT-003-12 케어리스트 기본 항목) (DMS)
- 결정1 위치: 포인트·케어미션은 `pet` 도메인(1층)에 둔다. 부르는 쪽 health(2층)·expense·community(3층)·scheduler(4층)가 모두 아래로 부르게 된다. 경로는 기존처럼 `/api`를 앞에 붙인다
- 결정2 보유 포인트: `MBR.HLD_PNT`는 `member` 도메인에 service가 아직 없어서 `pet/mapper/PointMapper`가 직접 읽고 바꾼다(PetMapper가 이미 MBR을 JOIN하는 것과 같다). `HLD_PNT`를 바꾸는 곳은 `PointService` 하나뿐이다. 바꿀 때는 `UPDATE MBR SET HLD_PNT = HLD_PNT + #{delta} WHERE MBR_SN = ? AND HLD_PNT + #{delta} >= 0` 한 문장으로 하고, 0행이면 `POINT_NOT_ENOUGH`다. 거래 후 잔액은 같은 트랜잭션에서 다시 읽어 `DLNG_AFTR_BLNC_PNT`에 넣는다
- 결정3 원장 부호와 멱등: `DLNG_PNT`는 적립이면 양수, 사용이면 음수로 저장한다(내역의 +/− 부호를 그대로 쓴다). 모든 적립·사용은 `IDMPT_DMND_ID`가 이미 있으면 아무것도 하지 않고 끝낸다(오류 아님). 동시에 같은 요청이 오면 UK가 막는다. ID 규칙은 아이템 구매 = 요청의 `idmptDmndId`, 미션 = `MSN-{dayMsnSn}`, 검진 = `CKUP-{petSn}-{yyyyMMdd}`
- 결정4 미션 마스터 (2026-10-27 SJM과 정리. 근거는 마이페이지 FN-MYP-020-03·05와 건강기록 FN-HLT-003-12. 시드 값은 기준이 아니다)
  - 두 풀의 역할: 일상케어는 매일 할 수 있는 것만 둔다. 하루 8개 중 과반인 5개는 반드시 오늘 할 수 있는 미션으로 채우려는 의도다. 활동기반은 할 수도 있지만 다 하지 못할 수도 있는 미션으로 변별력을 준다. 케어미션과 포인트의 목적은 사이트 활성화다
  - 케어리스트 미션: "토탈 케어리스트의 일상관리·정기관리 항목 체크"는 건강기록 명세의 서비스 기본 항목 11개(삭제·이름 변경 불가)를 가리킨다. 기본 항목 하나가 미션 하나다(화면설계서 20-1 예시 "양치하기", "귀 청소하기"와 같은 모양). 건강기록 명세에 종 구분이 없으므로 강아지·고양이 공통이다
  - 사용자가 추가한 일상관리·정기관리 항목(`CARE_ARTCL.BSC_ARTCL_YN = 'N'`)은 출제하지 않고, 체크해도 반영하지 않는다. 임의로 추가하고 체크해서 미션을 채우는 것을 막기 위해서다. 처방약·반복 예방 자동 항목도 기본 항목이 아니라서 넣지 않는다. 복용약이나 예방 관리가 없는 건강한 반려동물에게는 할 수 없는 미션이 되기 때문이다
  - 달성 기준 횟수는 모두 1회다. 식사량·음수량은 나이·종·자율급식에 따라 하루 횟수가 달라 서버가 기준을 정할 수 없으므로 그날 기록 1건이면 성공이다. 케어리스트 항목은 항목·날짜당 완료 기록이 1건이다. 여러 번 미션의 정산 규칙(결정7)은 명세대로 만들어 둔다
  - 일상케어(MYPC0010) 9개, 강아지·고양이 같음. 배점은 화면설계서 20-1 예시가 있으면 그 값, 없으면 수고에 맞춰 정했다(FN-MYP-020-05 "1~20P 차등")
    - 화장실 청소하기 10P: 강아지는 배변패드 교체, 고양이는 모래 속 대소변 치우기를 묶은 항목이라 다른 일상관리보다 손이 많이 간다
    - 양치하기 5P(예시), 빗질하기 5P, 발 관리하기 3P(가장 가벼움), 놀이하기 5P
    - 체중 기록하기 10P(예시, HLTC0014), 식사량 기록하기 10P(예시, HLTC0015), 음수량 기록하기 10P(HLTC0016), 배변 기록하기 5P(HLTC0017)
  - 활동기반(MYPC0011) 12개
    - 언제나 후보: 가계부 지출 등록하기 5P(예시), 케어 캘린더 일정 등록하기 5P(예시)
    - 언제나 후보, 정기관리 기본 항목 6개: 귀 청소하기 10P(예시), 발톱 관리하기 15P, 목욕하기 20P, 눈 주변 관리하기 5P, 발바닥 털 관리하기 10P, 구강 상태 확인하기 5P. 주기가 있어 매일 할 수 없으므로 일상케어가 아니라 활동기반에 둔다. 예정일과 관계없이 랜덤으로 출제하므로 health의 예정일 조회는 필요 없다. 예정일 전에 체크하면 다음 예정일이 그날부터 다시 계산되는 것(FN-HLT-003-12)은 그대로 둔다
    - 강아지만: 산책 기록하기 10P(예시, HLTC0018). 매일 하지만 날씨·시간에 따라 못 하는 날이 있고 실제로 나가야 하므로 활동기반에 둔다. 일상케어에 두면 강아지가 하루 최대 5P·평균 약 1.5P 더 받는다. 활동기반에 두면 강아지·고양이 하루 최대가 같고 평균 차이는 0.2P 이하다
    - 관찰 중인 항목이 있는 동물만: 관찰 기록 추가하기 10P(사진 등록 필수)
    - 대표 반려동물만: 펫이야기 글 등록하기 20P(명세), 펫이야기 댓글 작성하기 5P(명세 FN-MYP-020-03은 1P지만 5P로 바꾼다. 이미 하루 1회만 인정하므로 어뷰징은 막혀 있는데, 1P는 배점이 너무 낮아서다)
  - 하루 총 포인트(한 마리): 조건 없음 최대 90P·평균 약 63P, 대표면 최대 100P·평균 65P. 강아지·고양이 차이는 평균 0.2P 이하
  - 명세(DMS) 수정은 SJM이 한다: 일상케어에서 정기관리 분리, 산책을 활동기반으로, 사용자 추가·자동 항목 제외, 댓글 5P, 미션별 배점
  - 공용 DB: `CARE_MSN`에 시드 10행이 이미 있다(LJS의 `MasterSeeder`, 과거 `DAY_MSN`이 FK로 참조하므로 지우지 않음). 공용 DB용 `db/migration/20261027_02_update_care_msn_and_ckup_pnt.sql`에서 시드 행을 `MSN_NM`으로 찾아 위의 이름·풀·배점·횟수로 UPDATE하고, 없는 미션(케어리스트 기본 항목 11개, 관찰 기록 추가하기)을 INSERT한다. 시드 행과 대응: 케어리스트 완료하기는 대응하는 미션이 없으므로 `USE_YN='N'`으로 끈다, 배변 상태 기록하기→배변 기록하기, 산책 기록하기(활동기반 그대로), 커뮤니티 글 쓰기→펫이야기 글 등록하기, 댓글 3개 남기기→펫이야기 댓글 작성하기, 가계부 기록하기→가계부 지출 등록하기, 일정 등록하기→케어 캘린더 일정 등록하기, 체중·식사량·음수량은 이름 그대로. `MVMN_PATH`는 시드 값을 유지하고, 새 미션은 케어리스트 항목 `/health/calendar`, 관찰 `/health`
  - 새로 만드는 DB도 같게 `MasterSeeder.MISSIONS`를 위 21개로 맞춘다. `CharacterSeeder`(과거 DAY_MSN·원장 시드)는 고치지 않는다. PR에서 LJS에게 알린다
  - 같은 마이그레이션에서 MYPG03에 `MYPC0021` 검진기록 등록(SORT_SEQ 15, 명세 FN-MYP-020-08의 내역 표시명)을 INSERT한다. 부모는 `COM_CD`로 찾는다. 맨 위 주석에 근거(FN-MYP-020-03·05·08)와 되돌리기 SQL을 적는다. PR 본문에 "DB 변경 있음"과 파일 이름
  - 코드에서는 enum `pet/domain/CareMission`이 `MSN_NM`(UK)으로 행을 찾는다. CARE_MSN에 식별 코드 컬럼이 없어 컬럼을 늘리지 않으려는 것이다. 이름을 바꾸면 enum도 같이 바꿔야 한다. 출제 조건(강아지만·관찰 중·대표만)도 enum이 갖는다
- 결정5 출제(FN-MYP-020-03): 매일 00:00 KST에 주양육 관계(BRUP_REL PCG·`USE_YN='Y'`, PET `DEL_YN='N'`)마다 8개를 낸다. 일상케어 9개 중 랜덤 5개, 활동기반은 언제나 후보(가계부·일정·정기관리 6개, 강아지는 산책 포함)에 조건이 맞는 후보(관찰 중이면 관찰 기록, 대표면 펫이야기 글·댓글)를 합친 8~12개 중 랜덤 3개다. 활동기반 후보가 최소 8개라 명세의 "활동기반이 3개보다 적으면 일상케어에서 채움"은 생기지 않지만 규칙은 코드에 둔다. 대표 여부는 출제 시점의 `RPRS_PET_YN`으로 정한다. 8개를 섞어 넣은 순서가 출제 순서다(`DAY_MSN_SN` 오름차순). `DAY_MSN.MBR_SN`은 그때의 주양육자, `SCOR_PNT`는 `CMPN_PNT`를 복사한 값이다. 그날 미션이 이미 있는 동물은 건너뛰므로 다시 돌려도 안전하다
  - 관찰 중 항목 여부는 health(JAJ) 몫이고 pet은 health를 부를 수 없다. 그래서 배치는 `scheduler/CareMissionJob`에 두고 관찰 중인 반려동물 목록을 `CareMissionService.issue(ymd, obsrvPetSns)`에 넘긴다. health에 그 조회가 아직 없어서 `scheduler/ObservationFake`(빈 목록)로 먼저 만들고 나중에 연결한다
  - 배치는 매시 정각에 돈다(00:00 포함, 정산·출제 모두 이미 한 것은 건너뜀). 자정에 서버가 꺼져 있었던 날과 첫 배포 날도 그 뒤 첫 정각에 오늘 미션이 생긴다. 낮에 등록한 반려동물은 그날 다음 정각부터 미션을 받는다. (구현 때 고침: 처음에는 서버 시작(`ApplicationReadyEvent`) 때 한 번 돌리려 했으나, 테스트의 컨텍스트 로딩(`NyangmeongBackendApplicationTests`)이 DB 없이 뜨므로 기동 중 쿼리가 테스트를 깨뜨린다)
- 결정6 반영(FN-MYP-020-04·06): 다른 도메인이 부르는 메서드 두 개
  - `CareMissionService.reflect(Long petSn, CareMission msn, Long actorMbrSn)`: 오늘(KST) 그 동물에게 그 미션이 미완료로 있으면 `PRGRS_NMTM + 1`(행 잠금). 기준 횟수에 닿으면 `CMPTN_YN='Y'`, `CMPTN_DT`, `CMPTN_MBR_SN = actor`, `GIVE_PNT = SCOR_PNT`로 바꾸고 `DAY_MSN.MBR_SN`(주양육자)에게 즉시 적립한다. 미션이 없거나 이미 완료면 조용히 끝낸다. 권한 검사는 부르는 쪽이 이미 했다고 본다
  - `CareMissionService.reflectRepresentative(Long mbrSn, CareMission msn)`: 펫이야기용. 오늘 `DAY_MSN.MBR_SN = mbrSn`인 그 미션(대표 동물에게만 있음)에 같은 처리. 하루 1회와 실종센터 제외는 기준 1회와 부르는 쪽(KSJ)이 맡는다
  - 가계부 지출에 반려동물이 없으면(`PET_SN` null) 반영하지 않는다. 이것은 부르는 쪽 안내로 남긴다
  - 케어리스트 체크는 health(JAJ)가 기본 항목(`BSC_ARTCL_YN = 'Y'`)을 체크해 그날 완료 기록이 새로 생겼을 때만 그 항목의 미션으로 `reflect`를 부른다. 취소 뒤 다시 체크해 같은 기록이 되살아나는 경우는 부르지 않는다. 취소해도 오른 진행 횟수는 내리지 않는다. 이것도 부르는 쪽 안내로 남긴다
- 결정7 자정 정산(FN-MYP-020-05): 배치가 출제보다 먼저 전날 미션을 정산한다. `CLCLN_CFMTN_YN='N'`인 전날 미션 중 미완료는 진행률이 50%를 넘을 때만 `floor(SCOR_PNT × PRGRS_NMTM / ACHV_CRTR_NMTM)`를 적립하고(15P·2/3 → 10P), 50% 이하는 `GIVE_PNT = 0`이다. 완료와 미완료 모두 `CLCLN_CFMTN_YN='Y'`로 바꾼다. 삭제된 동물(PET `DEL_YN='Y'`)의 미션은 정산하지 않는다
- 결정8 원장 행(issues 마이페이지 13번): 미션 지급은 미션 하나에 원장 1행이다(`DAY_MSN_SN` FK가 1:1이라서). `MEMO_CN`에는 미션은 `{반려동물명} · {미션명}`, 검진은 반려동물명, 아이템 구매는 부르는 쪽이 준 문구(LJS, 예: "밀짚모자 외 1개")를 넣는다. 화면설계서 내역 행의 "날짜 · 내용"을 채우려고 `PointLedgerSummary`에 `regDt`(LocalDateTime, Y)와 `memoCn`(String, N)을 더한다. `DailyMissionSummary.givePnt`는 완료도 정산도 안 됐으면 null, 그 밖에는 값(0 포함)이다. 구현 전에 docs/api/pet.md 초안을 고치고 issues.md 마이페이지 13번에 이 결정을 적는다
- 결정9 검진 포인트(FN-MYP-020-08): `PointService.earnCheckupPoint(Long petSn, LocalDate ckupYmd)`. 검진일이 오늘−7일부터 오늘까지(양끝 포함)일 때만 20P를 그 동물의 주양육자에게 `MYPC0021`로 적립한다. 같은 동물·검진일은 결정3의 ID로 한 번만 준다. 삭제해도 회수하지 않는다. 지급했으면 true를 돌려준다. 부르는 쪽은 JAJ(검진 등록, 영수증 빠른 등록)다
- 결정10 아이템 구매 차감: `PointService.use(Long mbrSn, long amount, String idmptDmndId, String memoCn)`를 `MYPC0008`로 기록한다. 원장 1행이고 `HLD_ITEM_SN`은 null이다(구매는 여러 개를 한 번에 사므로). `PointFake`를 이것으로 바꾸는 일은 LJS의 "연결 남음"이라 이번에는 `ItemService`를 고치지 않는다. `PointService.getHldPnt(mbrSn)`도 같이 둔다
- FN-MYP-020-01 보유 포인트·FN-MYP-020-09 요약·FN-MYP-001-03·04: `GET /members/me/points` → `PointResponse`(초안 그대로). 월 적립·사용은 이번 달(KST) `PNT_GNLGR`의 양수·음수 합계와 건수다. `monthUsePnt`는 절댓값(양수)이다. 오늘 미션은 오늘 `DAY_MSN.MBR_SN = 나` JOIN PET `DEL_YN='N'`의 완료 수·전체 수·서로 다른 동물 수다. 바꿀 파일: `pet/controller/PointController`(`/api/members/me/points`)·`PointLedgerController`(`/api/point-ledgers`, 구현 때 더함: controller.md가 리소스 경로 전체를 클래스의 `@RequestMapping`에 두게 해서 경로가 다른 두 API를 한 클래스에 둘 수 없음), `pet/service/PointService`, `pet/mapper/PointMapper` + `mapper/pet/PointMapper.xml`, `pet/domain/PointLedger`·`PointDealType`(MYPC0007·0008·0009·0021), `pet/dto/PointResponse`. 부를 API: `GET /members/me/points`(SJM). 완료 조건 — 명령: `./gradlew build -q`(`PointServiceTest` 요약이 합계·건수·사용 절댓값을 돌려줌, `PointControllerTest` 200). 화면: 포인트 카드와 요약 3칸이 채워진다. 프론트 연결: SCR-MYP-020 포인트 카드·요약, SCR-MYP-001 포인트 요약 (프론트 저장소에서 /start)
- FN-MYP-020-02 적립·사용 내역: `GET /point-ledgers?yearMonth=yyyy-MM`(`YearMonth` + `@DateTimeFormat(pattern = "yyyy-MM")`, 형식이 틀리면 변환 실패로 `GlobalExceptionHandler`가 400, 없으면 이번 달. 구현 때 고침: `@Pattern`은 `HandlerMethodValidationException`이 나는데 지금 처리기가 받지 않아 500이 됨) → `List<PointLedgerSummary>`(초안 + 결정8의 두 필드). `REG_DT` 내림차순, 같으면 SN 내림차순. 유형 이름은 COM_CD JOIN. 미래 달이면 빈 목록이다. 월 합계는 프론트가 더한다(issues 18번 초안대로). 완료 조건 — 명령: `PointControllerTest` 형식이 틀린 `yearMonth`는 400, `PointServiceTest` 그 달 범위로 조회. 화면: 내역이 최신순, +/− 부호로 나온다. 프론트 연결: SCR-MYP-020 포인트 내역 (프론트 저장소에서 /start)
- FN-MYP-020-03·04 케어미션 탭·기록·날짜 이동: `GET /pets/{petSn}/daily-missions?msnCrtrYmd=`(없으면 오늘) → `List<DailyMissionSummary>`(초안 그대로, 출제 순서). 권한은 주양육자만(`PetAuthorizer.checkPrimary`, 탭이 주양육 동물만 보이므로). 지난 날은 정산된 값, 출제 전 날짜는 빈 목록이다. 탭 목록은 기존 `GET /pets`의 `brupRoleCd`로 프론트가 거른다. 바꿀 파일: `pet/controller/CareMissionController`, `pet/service/CareMissionService`, `pet/mapper/CareMissionMapper` + `mapper/pet/CareMissionMapper.xml`, `pet/domain/CareMission`·`DayMission`, `pet/dto/DailyMissionSummary`. 완료 조건 — 명령: `CareMissionServiceTest` 출제가 8개(일상케어 5·활동기반 3), 고양이는 산책 없음, 대표가 아니면 펫이야기 없음, 관찰 없으면 관찰 미션 없음, 사용자 추가 항목 미션 없음, 이미 있으면 건너뜀. `CareMissionControllerTest` 200, 공동양육자는 `PET_PRIMARY_ONLY`. 화면: 탭을 고르면 8개와 배점·완료 여부가 나오고 ◀▶로 지난 날이 나온다. 프론트 연결: SCR-MYP-020 오늘의 케어미션 탭·날짜 이동, SCR-MYP-001 오늘의 케어미션 요약 (프론트 저장소에서 /start)
- FN-MYP-020-05 지급 규칙: 결정6·7. 완료 조건 — 명령: `CareMissionServiceTest` 마지막 1회로 완료되면 즉시 적립·`GIVE_PNT = SCOR_PNT`, 정산 15P·2/3 → 10P, 1/2 → 0, 완료분은 다시 적립하지 않음, 정산을 두 번 돌려도 한 번만 적립
- FN-MYP-020-06 공동양육자 수행분: 결정6. 완료 조건 — 명령: `CareMissionServiceTest` 공동양육자가 반영하면 `CMPTN_MBR_SN`은 공동양육자, 적립은 주양육자
- FN-MYP-020-07 0마리 안내: 서버 일은 없다. 주양육 동물이 없으면 `GET /pets`에 PCG가 없고 요약의 `todayMsnPetCnt`가 0이다. 프론트가 안내를 띄운다
- FN-MYP-020-08 검진 포인트: 결정9. 완료 조건 — 명령: `PointServiceTest` 같은 동물·검진일 두 번 요청 시 한 번만 적립, 8일 전 검진일은 적립 안 함, 주양육자에게 적립. 화면: (연결 뒤) 내역에 "검진기록 등록 +20P". 연결 남음: 검진기록 등록·영수증 빠른 등록에서 `earnCheckupPoint` 호출 (JAJ)
- 차감(아이템 구매용): 결정10. 완료 조건 — 명령: `PointServiceTest` 잔액보다 크면 `POINT_NOT_ENOUGH`이고 원장이 없음, 같은 `idmptDmndId` 두 번이면 한 번만 차감
- 자정 배치: `scheduler/CareMissionJob`(`@Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")`, 매시 정각): 전날 정산 → 오늘 출제. `scheduler/ObservationFake`. 완료 조건 — 명령: `CareMissionJobTest` 정산을 출제보다 먼저 부르고 가짜 관찰 목록을 넘긴다
- 하지 않는 것: `PointFake` → `PointService` 연결(LJS), 각 활동에서 `reflect`·`earnCheckupPoint`를 부르는 것(JAJ·KSJ·가계부 담당), 관찰 중 동물 조회(JAJ, 가짜), 대표 동물 삭제 때 당일 미션 삭제(SCR-MYP-002), 관리자 포인트 조정(MYPC0009), 지출 합계(SCR-MYP-030)
- 새 의존성: 없음

### 추가 요청
| 추가-1 | `scheduler/CareMissionJob`(`@ConditionalOnProperty(name = "app.care-mission.batch-enabled", havingValue = "true")`), `application-prod.properties`(`app.care-mission.batch-enabled=true`), `application.properties.example`(배치 구획에 주석 줄로 키 안내, 생략하면 false), 새 테스트 `scheduler/CareMissionJobSwitchTest` | 명령: `./gradlew build -q`, `CareMissionJobSwitchTest` 키가 없거나 false면 `CareMissionJob` 빈이 없고 true면 있음. 화면: 키 없이 로컬 `bootRun`하면 정각이 지나도 `DAY_MSN`에 행이 생기지 않음 | 요청 2026-10-27 16:40 "배치 스위치 추가: 로컬 기본 꺼짐, 배포(prod)에서 켜짐, 확인할 때만 로컬에서 켬. 팀원 PC마다 배치가 공용 DB에 쓰지 않게" |

## 다음에 할 것
- SCR-MYP-020 병합 뒤: PR(본문에 "DB 변경 있음" `db/migration/20261027_02_update_care_msn_and_ckup_pnt.sql`, `MasterSeeder` 변경 LJS에 알림) → 병합 직후 공용 DB에 실행(아직이면 `20261026_01_update_petg03_colors.sql`도), PR에 완료 댓글 → 사람 확인 3-1(로컬 `app.care-mission.batch-enabled=true`로 띄워 정각 뒤 `GET /api/pets/{petSn}/daily-missions` 8개, 확인 뒤 키 지움)·추가-1(키 없이 정각을 넘겨 `DAY_MSN` 오늘 행 수 그대로) → `/end`에서 `확인`해 `끝`
- 연결 남음: 프론트 SCR-MYP-020 포인트 카드·요약·내역·케어미션 탭과 SCR-MYP-001 요약(프론트 저장소에서 /start, SCR-MYP-003은 체중 잠금은 `GET /pets/{petSn}/daily-health-records?dailyRcdIdctCd=HLTC0014&size=1`(JAJ)로 판단, 기본 선택 동물은 `rprsPetYn`, 털색 드롭다운 설명), JAJ 건강 일지 기록·케어리스트 기본 항목 체크·일정·관찰 기록 → `reflect`, 검진기록·영수증 빠른 등록 → `earnCheckupPoint`, 관찰 중 반려동물 조회(`scheduler/ObservationFake` 대체), KSJ 펫이야기 글·댓글 → `reflectRepresentative`, LJS `PointFake` → `PointService.use`, 가계부 지출 → `reflect`(반려동물 있는 지출만)
- 건강기록 기본 항목 "발 관리"의 이름을 JAJ와 정하기(후보: 발바닥 확인하기). 발톱 관리·발바닥 털 관리와 겹쳐 보이고 뜻이 넓다. 바뀌면 마이그레이션으로 `CARE_MSN` 미션 이름도 맞춘다
