package com.nyangmeong.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.ai.domain.AiConversationMessage;
import com.nyangmeong.ai.domain.AiConversationRoom;

/**
 * [AI 대화 Mapper]
 */
@Mapper
public interface AiConversationMapper {

    /**
     * 본인 대화방 목록 조회 (반려동물 태그 없으면 전체)
     */
    List<AiConversationRoom> selectRoomList(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn);

    /**
     * 본인 대화방 단건 조회
     */
    AiConversationRoom selectRoom(@Param("mbrSn") Long mbrSn, @Param("aiCnvrsRoomSn") Long aiCnvrsRoomSn);

    /**
     * 본인 대화방 잠금 조회 (메시지 순서 부여용)
     */
    AiConversationRoom selectRoomForUpdate(@Param("mbrSn") Long mbrSn, @Param("aiCnvrsRoomSn") Long aiCnvrsRoomSn);

    /**
     * 대화방의 메시지 목록 조회
     */
    List<AiConversationMessage> selectMessageList(Long aiCnvrsRoomSn);

    /**
     * 메시지 단건 조회
     */
    AiConversationMessage selectMessage(Long aiCnvrsMsgSn);

    /**
     * 다음 메시지 순서 조회
     */
    int selectNextMsgSeq(Long aiCnvrsRoomSn);

    /**
     * 대화방 등록
     */
    int insertRoom(AiConversationRoom room);

    /**
     * 메시지 등록
     */
    int insertMessage(AiConversationMessage message);

    /**
     * 본인 대화방 삭제 (메시지는 CASCADE)
     */
    int deleteRoom(@Param("mbrSn") Long mbrSn, @Param("aiCnvrsRoomSn") Long aiCnvrsRoomSn);

    /**
     * 회원들의 대화방 일괄 삭제
     */
    int deleteRoomsByMbrSns(@Param("mbrSns") List<Long> mbrSns);
}
