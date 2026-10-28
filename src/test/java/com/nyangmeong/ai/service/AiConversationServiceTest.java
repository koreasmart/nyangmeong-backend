package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.ai.domain.AiConversationMessage;
import com.nyangmeong.ai.domain.AiConversationRoom;
import com.nyangmeong.ai.dto.ConversationCreateRequest;
import com.nyangmeong.ai.dto.ConversationMessageCreateRequest;
import com.nyangmeong.ai.dto.ConversationMessageResponse;
import com.nyangmeong.ai.dto.ConversationResponse;
import com.nyangmeong.ai.dto.ConversationSummary;
import com.nyangmeong.ai.mapper.AiConversationMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

/**
 * [검사 - AI 대화 저장·조회·메시지 추가·삭제·파기]
 */
class AiConversationServiceTest {

    private static final Long MBR = 1L;
    private static final Long ROOM = 7L;
    private static final Long PET = 3L;

    private final AiConversationMapper aiConversationMapper = mock(AiConversationMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final AiConversationService service = new AiConversationService(aiConversationMapper, petAuthorizer,
            fieldCryptoService);

    AiConversationServiceTest() {
        when(fieldCryptoService.encrypt(any())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
        when(fieldCryptoService.decrypt(any())).thenAnswer(inv -> {
            String envelope = inv.getArgument(0);
            return envelope.substring(4, envelope.length() - 1);
        });
    }

    private static AiConversationRoom room(Long petSn) {
        AiConversationRoom room = new AiConversationRoom();
        room.setAiCnvrsRoomSn(ROOM);
        room.setMbrSn(MBR);
        room.setPetSn(petSn);
        room.setCnvrsTtlEnc("ENC(제목)");
        room.setRegDt(LocalDateTime.of(2026, 10, 27, 10, 0));
        return room;
    }

    private static AiConversationMessage message(Long sn, String roleCd, int msgSeq, String cmptnYn) {
        AiConversationMessage message = new AiConversationMessage();
        message.setAiCnvrsMsgSn(sn);
        message.setAiCnvrsRoomSn(ROOM);
        message.setRoleCd(roleCd);
        message.setCnvrsCnEnc("ENC(내용" + msgSeq + ")");
        message.setMsgSeq(msgSeq);
        message.setCmptnYn(cmptnYn);
        message.setRegDt(LocalDateTime.of(2026, 10, 27, 10, msgSeq));
        return message;
    }

    @Test
    void 대화를_만들면_제목과_첫_메시지가_암호문으로_저장된다() {
        doAnswer(inv -> {
            AiConversationRoom saved = inv.getArgument(0);
            saved.setAiCnvrsRoomSn(ROOM);
            return 1;
        }).when(aiConversationMapper).insertRoom(any());
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(room(PET));

        ConversationSummary result = service.create(MBR, new ConversationCreateRequest(PET, "제목", "첫 질문"));

        ArgumentCaptor<AiConversationRoom> roomCaptor = ArgumentCaptor.forClass(AiConversationRoom.class);
        verify(aiConversationMapper).insertRoom(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(roomCaptor.getValue().getCnvrsTtlEnc()).isEqualTo("ENC(제목)");
        ArgumentCaptor<AiConversationMessage> msgCaptor = ArgumentCaptor.forClass(AiConversationMessage.class);
        verify(aiConversationMapper).insertMessage(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getAiCnvrsRoomSn()).isEqualTo(ROOM);
        assertThat(msgCaptor.getValue().getRoleCd()).isEqualTo("AIMC0024");
        assertThat(msgCaptor.getValue().getCnvrsCnEnc()).isEqualTo("ENC(첫 질문)");
        assertThat(msgCaptor.getValue().getMsgSeq()).isEqualTo(1);
        assertThat(msgCaptor.getValue().getCmptnYn()).isEqualTo("Y");
        assertThat(result.cnvrsTtl()).isEqualTo("제목");
        assertThat(result.petSn()).isEqualTo(PET);
    }

    @Test
    void 태그가_없는_대화는_반려동물_권한을_검사하지_않는다() {
        doAnswer(inv -> {
            AiConversationRoom saved = inv.getArgument(0);
            saved.setAiCnvrsRoomSn(ROOM);
            return 1;
        }).when(aiConversationMapper).insertRoom(any());
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(room(null));

        service.create(MBR, new ConversationCreateRequest(null, "제목", "첫 질문"));

        verify(petAuthorizer, never()).checkCaregiver(anyLong(), anyLong());
    }

    @Test
    void 양육하지_않는_반려동물이면_NOT_FOUND이고_대화를_만들지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, PET);

        assertThatThrownBy(() -> service.create(MBR, new ConversationCreateRequest(PET, "제목", "첫 질문")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(aiConversationMapper, never()).insertRoom(any());
    }

    @Test
    void 없는_반려동물_태그로_목록을_조회하면_NOT_FOUND다() {
        doThrow(new CustomException(ErrorCode.PET_NOT_FOUND)).when(petAuthorizer).checkCaregiver(MBR, 99L);

        assertThatThrownBy(() -> service.search(MBR, 99L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(aiConversationMapper, never()).selectRoomList(anyLong(), any());
    }

    @Test
    void 목록은_제목을_복호화해_돌려준다() {
        when(aiConversationMapper.selectRoomList(MBR, null)).thenReturn(List.of(room(null)));

        List<ConversationSummary> result = service.search(MBR, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cnvrsTtl()).isEqualTo("제목");
    }

    @Test
    void 반려동물_태그로_목록을_조회하면_권한을_먼저_검사한다() {
        when(aiConversationMapper.selectRoomList(MBR, PET)).thenReturn(List.of());

        service.search(MBR, PET);

        verify(petAuthorizer).checkCaregiver(MBR, PET);
    }

    @Test
    void 상세는_메시지를_복호화해_순서대로_돌려준다() {
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(room(PET));
        when(aiConversationMapper.selectMessageList(ROOM)).thenReturn(
                List.of(message(10L, "AIMC0024", 1, "Y"), message(11L, "AIMC0025", 2, "N")));

        ConversationResponse result = service.getConversation(MBR, ROOM);

        assertThat(result.cnvrsTtl()).isEqualTo("제목");
        assertThat(result.messages()).extracting(ConversationMessageResponse::cnvrsCn)
                .containsExactly("내용1", "내용2");
        assertThat(result.messages()).extracting(ConversationMessageResponse::cmptnYn).containsExactly(true, false);
    }

    @Test
    void 남의_대화나_없는_번호의_상세는_NOT_FOUND다() {
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(null);

        assertThatThrownBy(() -> service.getConversation(MBR, ROOM)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void 메시지를_추가하면_다음_순서로_저장한다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, ROOM)).thenReturn(room(PET));
        when(aiConversationMapper.selectNextMsgSeq(ROOM)).thenReturn(2);
        doAnswer(inv -> {
            AiConversationMessage saved = inv.getArgument(0);
            saved.setAiCnvrsMsgSn(11L);
            return 1;
        }).when(aiConversationMapper).insertMessage(any());
        when(aiConversationMapper.selectMessage(11L)).thenReturn(message(11L, "AIMC0025", 2, "N"));

        ConversationMessageResponse result = service.createMessage(MBR, ROOM,
                new ConversationMessageCreateRequest("AIMC0025", "내용2", false));

        ArgumentCaptor<AiConversationMessage> captor = ArgumentCaptor.forClass(AiConversationMessage.class);
        verify(aiConversationMapper).insertMessage(captor.capture());
        assertThat(captor.getValue().getMsgSeq()).isEqualTo(2);
        assertThat(captor.getValue().getCmptnYn()).isEqualTo("N");
        assertThat(captor.getValue().getCnvrsCnEnc()).isEqualTo("ENC(내용2)");
        assertThat(result.msgSeq()).isEqualTo(2);
    }

    @Test
    void 완료_여부를_보내지_않으면_완료로_저장한다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, ROOM)).thenReturn(room(PET));
        when(aiConversationMapper.selectNextMsgSeq(ROOM)).thenReturn(2);
        doAnswer(inv -> {
            AiConversationMessage saved = inv.getArgument(0);
            saved.setAiCnvrsMsgSn(11L);
            return 1;
        }).when(aiConversationMapper).insertMessage(any());
        when(aiConversationMapper.selectMessage(11L)).thenReturn(message(11L, "AIMC0024", 2, "Y"));

        service.createMessage(MBR, ROOM, new ConversationMessageCreateRequest("AIMC0024", "내용2", null));

        ArgumentCaptor<AiConversationMessage> captor = ArgumentCaptor.forClass(AiConversationMessage.class);
        verify(aiConversationMapper).insertMessage(captor.capture());
        assertThat(captor.getValue().getCmptnYn()).isEqualTo("Y");
    }

    @Test
    void 남의_대화에는_메시지를_추가할_수_없다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, ROOM)).thenReturn(null);

        assertThatThrownBy(() -> service.createMessage(MBR, ROOM,
                new ConversationMessageCreateRequest("AIMC0024", "내용", null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(aiConversationMapper, never()).insertMessage(any());
    }

    @Test
    void 알_수_없는_역할_코드는_입력값_오류다() {
        assertThatThrownBy(() -> service.createMessage(MBR, ROOM,
                new ConversationMessageCreateRequest("XXXX", "내용", null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 삭제는_본인_대화만_지우고_없으면_NOT_FOUND다() {
        when(aiConversationMapper.deleteRoom(MBR, ROOM)).thenReturn(1);
        service.delete(MBR, ROOM);

        when(aiConversationMapper.deleteRoom(MBR, 99L)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(MBR, 99L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void 파기_대상이_없으면_삭제를_부르지_않는다() {
        assertThat(service.deleteByMbrSns(List.of())).isZero();

        verify(aiConversationMapper, never()).deleteRoomsByMbrSns(any());
    }

    @Test
    void 파기_대상_회원들의_대화를_지운다() {
        when(aiConversationMapper.deleteRoomsByMbrSns(List.of(5L, 6L))).thenReturn(4);

        assertThat(service.deleteByMbrSns(List.of(5L, 6L))).isEqualTo(4);
    }
}
