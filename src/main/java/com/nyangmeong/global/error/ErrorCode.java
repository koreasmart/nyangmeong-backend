package com.nyangmeong.global.error;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

/**
 * [공통 API 에러 코드]
 * - HTTP 상태 코드 + 사용자 메시지를 한 곳에서 관리
 * - 도메인 담당은 자기 구획 안에만 추가 (맨 끝 추가 시 병합 충돌, docs/convention/shared-files.md)
 * - 응답 JSON 의 code 는 상수 이름(name())
 */
@RequiredArgsConstructor
public enum ErrorCode {

    /** 공통(global). 입력값 검증 오류는 INVALID_INPUT_VALUE 하나, 틀린 항목은 응답 data의 필드 목록 */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기는 10MB 이하여야 합니다."),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용하지 않는 파일 형식입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    /** 401은 토큰이 없거나 틀리거나 만료된 경우만. 권한 부족과 정지·탈퇴 회원은 403 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 내용만 수정하거나 삭제할 수 있습니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다."),
    COMMON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공통코드입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용하지 않는 요청 방식입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "요청 식별값(Idempotency-Key)이 필요합니다."),
    /** 멱등 요청 ID(idmptDmndId)가 같은 요청이 처리 중이거나 끝난 경우 */
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    /** member */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "일치하는 회원정보를 찾을 수 없습니다."),
    /** 이메일·비밀번호 불일치는 토큰 문제가 아니라 400 */
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "일치하는 회원정보를 찾을 수 없습니다."),
    LOGIN_CAPTCHA_REQUIRED(HttpStatus.TOO_MANY_REQUESTS,
            "로그인 5회 실패로 보안코드 확인이 필요합니다. 보안코드를 입력해 다시 로그인해 주세요."),
    CAPTCHA_MISMATCH(HttpStatus.BAD_REQUEST, "보안코드가 일치하지 않습니다. 다시 입력해 주세요."),
    CAPTCHA_NOT_REQUIRED(HttpStatus.BAD_REQUEST, "보안코드 확인이 필요하지 않습니다."),
    MEMBER_INACTIVE(HttpStatus.FORBIDDEN, "이용할 수 없는 계정입니다. 고객센터에 문의해 주세요."),
    /** 비밀번호가 맞은 탈퇴 회원 : 프론트가 재가입·복구를 안내 (MEMBER_INACTIVE 는 랜딩 이동이라 따로 둠) */
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다. 재가입 또는 계정 복구를 진행해 주세요."),
    SNS_ONLY_MEMBER(HttpStatus.BAD_REQUEST, "SNS로 가입한 계정입니다. SNS 로그인을 이용해 주세요."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 기존에 가입한 방법으로 로그인해 주세요."),
    /** 탈퇴 후 보관 중인 이메일 : 새 회원을 만들지 않고 복구 흐름으로 안내 */
    MEMBER_RECOVERY_AVAILABLE(HttpStatus.CONFLICT, "탈퇴 후 보관 중인 계정입니다. 기존 정보 복구를 진행해 주세요."),
    NICKNAME_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."),
    PHONE_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 휴대전화번호입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "휴대전화번호 형식을 확인해 주세요."),
    EMAIL_CERT_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 정보를 찾을 수 없습니다. 인증번호를 다시 요청해 주세요."),
    EMAIL_CERT_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    EMAIL_CERT_EXPIRED(HttpStatus.BAD_REQUEST, "인증시간이 만료되었습니다. 다시 보내기를 눌러 주세요."),
    EMAIL_CERT_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 완료해 주세요."),
    EMAIL_CERT_FAIL_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증번호를 여러 번 잘못 입력했습니다. 인증번호를 다시 받아 주세요."),
    EMAIL_CERT_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용한 인증번호입니다."),
    EMAIL_CERT_RESEND_NOT_YET(HttpStatus.BAD_REQUEST, "인증번호를 다시 보낼 수 없습니다. 잠시 후 시도해 주세요."),
    PASSWORD_RESET_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "비밀번호 재설정 권한이 만료되었거나 이미 사용되었습니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 합니다."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "약관을 찾을 수 없습니다."),
    WITHDRAW_PHRASE_MISMATCH(HttpStatus.BAD_REQUEST, "탈퇴 확인 문구가 일치하지 않습니다."),
    MEMBER_RECOVERY_EXPIRED(HttpStatus.BAD_REQUEST, "복구 가능 기간이 지난 계정입니다."),
    DASHBOARD_WIDGET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "메인에 표시할 위젯은 최대 5개입니다."),

    /** pet */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물 정보를 찾을 수 없습니다."),
    PET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "양육 중인 반려동물만 볼 수 있습니다."),
    PET_PRIMARY_ONLY(HttpStatus.FORBIDDEN, "주양육자만 할 수 있습니다."),
    PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "반려동물은 계정당 10마리까지 등록할 수 있습니다."),
    PET_WEIGHT_LOCKED(HttpStatus.BAD_REQUEST, "건강 일지에 체중 기록이 있어 체중을 직접 수정할 수 없습니다."),
    BREED_NOT_FOUND(HttpStatus.NOT_FOUND, "품종을 찾을 수 없습니다."),
    CAREGIVER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "공동양육자는 반려동물당 5명까지입니다."),
    CAREGIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "공동양육 정보를 찾을 수 없습니다."),
    CAREGIVER_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    CAREGIVER_INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 초대입니다."),
    CAREGIVER_INVITATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 수락하거나 거절한 초대입니다."),
    PIXEL_CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "캐릭터를 찾을 수 없습니다."),
    PIXEL_CHARACTER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 캐릭터가 있는 반려동물입니다."),
    PIXEL_CHARACTER_CREATING(HttpStatus.CONFLICT, "캐릭터를 생성하는 중입니다. 잠시 후 다시 시도해 주세요."),
    PIXEL_CHARACTER_UNDER_REVIEW(HttpStatus.BAD_REQUEST, "오류 확인 중인 캐릭터는 꾸밀 수 없습니다."),
    PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED(HttpStatus.CONFLICT, "이미 오류 문의가 접수된 캐릭터입니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."),
    ITEM_ALREADY_HELD(HttpStatus.CONFLICT, "이미 보유한 아이템입니다."),
    ITEM_NOT_HELD(HttpStatus.BAD_REQUEST, "보유하지 않은 아이템은 착용할 수 없습니다."),
    ITEM_SLOT_DUPLICATED(HttpStatus.BAD_REQUEST, "같은 부위에는 아이템을 하나만 착용할 수 있습니다."),
    POINT_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),

    /** health */
    CARE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "케어 항목을 찾을 수 없습니다."),
    CARE_ITEM_DEFAULT_LIMITED(HttpStatus.BAD_REQUEST, "기본 케어 항목은 주기만 수정할 수 있습니다."),
    CARE_ITEM_DEFAULT_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "기본 케어 항목은 삭제할 수 없습니다."),
    CARE_ITEM_MAIN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "메인에 표시할 케어 항목은 최대 6개입니다."),
    CARE_COMPLETION_NOT_FOUND(HttpStatus.NOT_FOUND, "완료 기록을 찾을 수 없습니다."),
    CARE_COMPLETION_FUTURE_DATE(HttpStatus.BAD_REQUEST, "완료일은 오늘 이후로 입력할 수 없습니다."),
    MEDICATION_ALREADY_CHECKED(HttpStatus.CONFLICT, "오늘 투약은 이미 체크했습니다."),
    CARE_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    CHECKUP_NOT_FOUND(HttpStatus.NOT_FOUND, "검진기록을 찾을 수 없습니다."),
    CHECKUP_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다."),
    CHECKUP_ATTACHMENT_NOT_ANALYZABLE(HttpStatus.BAD_REQUEST, "검사결과지 PDF만 분석할 수 있습니다."),
    CHECKUP_ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT, "이미 분석을 요청한 파일입니다."),
    DAILY_HEALTH_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "건강 일지 기록을 찾을 수 없습니다."),
    ETC_INSPECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "기타검사를 찾을 수 없습니다."),
    INSPECTION_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "검사 수치를 찾을 수 없습니다."),
    INSPECTION_VALUE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "같은 검진기록에 이미 등록된 검사 항목입니다."),
    OBSERVATION_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "관찰 항목을 찾을 수 없습니다."),
    OBSERVATION_ITEM_ENDED(HttpStatus.BAD_REQUEST, "종료된 관찰 항목에는 회차를 추가할 수 없습니다."),
    OBSERVATION_CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "관찰 회차를 찾을 수 없습니다."),
    PRESCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "처방약을 찾을 수 없습니다."),
    REPEAT_PREVENTION_NOT_FOUND(HttpStatus.NOT_FOUND, "반복 예방 관리 항목을 찾을 수 없습니다."),

    /** place */
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    PLACE_BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기를 찾을 수 없습니다."),
    PLACE_BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 즐겨찾기한 장소입니다."),

    /** notification */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    /** expense */
    EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "지출 내역을 찾을 수 없습니다."),
    EXPENSE_PET_REQUIRED(HttpStatus.BAD_REQUEST, "병원 카테고리는 반려동물을 선택해야 합니다."),
    EXPENSE_LIMIT_NOT_FOUND(HttpStatus.NOT_FOUND, "월 지출 한도가 설정되지 않았습니다."),
    /** 영수증 인식(docs/api/receipt.md). 도메인 확정 뒤 그 구획으로 이동 */
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."),
    RECEIPT_RECOGNITION_FAILED(HttpStatus.BAD_REQUEST, "영수증을 인식하지 못했습니다. 다시 촬영해 주세요."),
    RECEIPT_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소한 영수증입니다."),
    RECEIPT_ALREADY_SAVED(HttpStatus.CONFLICT, "이미 저장한 영수증입니다."),
    RECEIPT_ANALYZING(HttpStatus.CONFLICT, "영수증을 분석하는 중입니다. 잠시 후 다시 시도해 주세요."),
    RECEIPT_NOT_SAVABLE(HttpStatus.CONFLICT, "인식하지 못한 영수증은 저장할 수 없습니다. 다시 촬영해 주세요."),

    /** community */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 최대 10장까지 올릴 수 있습니다."),
    POST_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 글입니다."),
    REPORT_COMMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 댓글입니다."),
    LOST_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "실종 제보를 찾을 수 없습니다."),
    LOST_REPORT_ALREADY_FOUND(HttpStatus.CONFLICT, "이미 찾았어요 처리된 제보입니다."),
    LOST_REPORT_EXPIRED(HttpStatus.BAD_REQUEST, "게시 기간이 끝난 제보는 갱신할 수 없습니다."),
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "뉴스를 찾을 수 없습니다."),
    CARE_RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "펫돌봄 모집글을 찾을 수 없습니다."),
    CARE_RECRUITMENT_CLOSED(HttpStatus.CONFLICT, "마감된 모집글입니다."),
    CARE_WAGE_BELOW_MINIMUM(HttpStatus.BAD_REQUEST, "돌봄 비용은 최저시급 이상이어야 합니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "채팅방 참여자만 볼 수 있습니다."),
    CHAT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자신의 글에는 채팅을 시작할 수 없습니다."),
    CARE_MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 정보를 찾을 수 없습니다."),
    CARE_MATCH_OWNER_ONLY(HttpStatus.FORBIDDEN, "모집글 작성자만 할 수 있습니다."),
    CARE_MATCH_PROVIDER_ONLY(HttpStatus.FORBIDDEN, "돌봄 제공자만 할 수 있습니다."),
    CARE_MATCH_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 매칭이 확정된 모집글입니다."),
    CARE_MATCH_INVALID_STATUS(HttpStatus.CONFLICT, "현재 매칭 상태에서는 할 수 없는 요청입니다."),

    /** support */
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
    INQUIRY_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "접수 상태의 문의만 수정할 수 있습니다."),
    INQUIRY_ATTACHMENT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "첨부 파일은 최대 10개까지 올릴 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    /** 사용자 메시지 */
    public String message() {
        return message;
    }

    /** http 상태 객체 */
    public HttpStatus status() {
        return status;
    }

    /** http 상태 코드 숫자 (응답 JSON 의 code 와 다름) */
    public int code() {
        return status.value();
    }
}
