package com.nyangmeong.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.domain.AiConversationMessage;
import com.nyangmeong.ai.domain.AiConversationRoom;
import com.nyangmeong.ai.domain.AiMessageRole;
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

import lombok.RequiredArgsConstructor;

/**
 * [AI 대화 서비스]
 * - SCR-AIM-011 대화 저장·조회·삭제, Python AI 서버가 /internal/ai/** 로 호출
 * - 제목·내용은 FieldCryptoService 로 암호화해 저장, 남의 대화는 없는 대화와 같은 NOT_FOUND
 */
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationMapper aiConversationMapper;
    private final PetAuthorizer petAuthorizer;
    private final FieldCryptoService fieldCryptoService;

    /**
     * 대화 생성 (방과 첫 메시지)
     */
    @Transactional
    public ConversationSummary create(Long mbrSn, ConversationCreateRequest req) {
        checkPetTag(mbrSn, req.petSn());
        AiConversationRoom room = new AiConversationRoom();
        room.setMbrSn(mbrSn);
        room.setPetSn(req.petSn());
        room.setCnvrsTtlEnc(fieldCryptoService.encrypt(req.cnvrsTtl()));
        aiConversationMapper.insertRoom(room);

        insertMessage(room.getAiCnvrsRoomSn(), AiMessageRole.USER, req.cnvrsCn(), true, 1);
        return toSummary(getOwnedRoomOrThrow(mbrSn, room.getAiCnvrsRoomSn()));
    }

    /**
     * 대화 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ConversationSummary> search(Long mbrSn, Long petSn) {
        checkPetTag(mbrSn, petSn);
        return aiConversationMapper.selectRoomList(mbrSn, petSn).stream().map(this::toSummary).toList();
    }

    /**
     * 대화 상세 조회
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long mbrSn, Long aiCnvrsRoomSn) {
        AiConversationRoom room = getOwnedRoomOrThrow(mbrSn, aiCnvrsRoomSn);
        List<ConversationMessageResponse> messages = aiConversationMapper.selectMessageList(aiCnvrsRoomSn).stream()
                .map(this::toMessageResponse).toList();
        return new ConversationResponse(room.getAiCnvrsRoomSn(), room.getPetSn(),
                fieldCryptoService.decrypt(room.getCnvrsTtlEnc()), room.getRegDt(), messages);
    }

    /**
     * 대화 메시지 추가
     */
    @Transactional
    public ConversationMessageResponse createMessage(Long mbrSn, Long aiCnvrsRoomSn,
            ConversationMessageCreateRequest req) {
        AiMessageRole role = AiMessageRole.USER.is(req.roleCd()) ? AiMessageRole.USER
                : AiMessageRole.AI.is(req.roleCd()) ? AiMessageRole.AI : null;
        if (role == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        /** 방 행 잠금으로 같은 대화의 동시 저장 순서 충돌 방지 */
        if (aiConversationMapper.selectRoomForUpdate(mbrSn, aiCnvrsRoomSn) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        int msgSeq = aiConversationMapper.selectNextMsgSeq(aiCnvrsRoomSn);
        AiConversationMessage message = insertMessage(aiCnvrsRoomSn, role, req.cnvrsCn(),
                !Boolean.FALSE.equals(req.cmptnYn()), msgSeq);
        return toMessageResponse(aiConversationMapper.selectMessage(message.getAiCnvrsMsgSn()));
    }

    /**
     * 대화 삭제 (물리 삭제)
     */
    @Transactional
    public void delete(Long mbrSn, Long aiCnvrsRoomSn) {
        if (aiConversationMapper.deleteRoom(mbrSn, aiCnvrsRoomSn) == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    /**
     * 회원들의 대화 파기 (탈퇴 보관일수 경과분)
     */
    @Transactional
    public int deleteByMbrSns(List<Long> mbrSns) {
        if (mbrSns.isEmpty()) {
            return 0;
        }
        return aiConversationMapper.deleteRoomsByMbrSns(mbrSns);
    }

    /**
     * 펫 태그 검사, 없는 반려동물과 남의 반려동물은 같은 NOT_FOUND
     */
    private void checkPetTag(Long mbrSn, Long petSn) {
        if (petSn == null) {
            return;
        }
        try {
            petAuthorizer.checkCaregiver(mbrSn, petSn);
        } catch (CustomException e) {
            /** 존재 여부를 알려 주지 않기 위해 코드를 통일 */
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    /**
     * 메시지 암호화 저장
     */
    private AiConversationMessage insertMessage(Long aiCnvrsRoomSn, AiMessageRole role, String cnvrsCn,
            boolean cmptn, int msgSeq) {
        AiConversationMessage message = new AiConversationMessage();
        message.setAiCnvrsRoomSn(aiCnvrsRoomSn);
        message.setRoleCd(role.getCode());
        message.setCnvrsCnEnc(fieldCryptoService.encrypt(cnvrsCn));
        message.setMsgSeq(msgSeq);
        message.setCmptnYn(cmptn ? "Y" : "N");
        aiConversationMapper.insertMessage(message);
        return message;
    }

    /**
     * 본인 대화방 조회, 없으면 NOT_FOUND
     */
    private AiConversationRoom getOwnedRoomOrThrow(Long mbrSn, Long aiCnvrsRoomSn) {
        AiConversationRoom room = aiConversationMapper.selectRoom(mbrSn, aiCnvrsRoomSn);
        if (room == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return room;
    }

    private ConversationSummary toSummary(AiConversationRoom room) {
        return new ConversationSummary(room.getAiCnvrsRoomSn(), room.getPetSn(),
                fieldCryptoService.decrypt(room.getCnvrsTtlEnc()), room.getRegDt());
    }

    private ConversationMessageResponse toMessageResponse(AiConversationMessage message) {
        return new ConversationMessageResponse(message.getAiCnvrsMsgSn(), message.getRoleCd(),
                fieldCryptoService.decrypt(message.getCnvrsCnEnc()), message.getMsgSeq(),
                "Y".equals(message.getCmptnYn()), message.getRegDt());
    }
}
