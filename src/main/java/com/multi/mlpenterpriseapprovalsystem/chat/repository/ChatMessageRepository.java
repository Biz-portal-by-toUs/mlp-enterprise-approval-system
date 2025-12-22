package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 컬렉션 접근 repository
 *
 * @author : 김승기
 * @filename : ChatMessageRepository
 * @since : 2025. 12. 16. 화요일
 */
public interface ChatMessageRepository
        extends MongoRepository<ChatMessage, String> {

    // 최초 진입 (최근 메시지)
    List<ChatMessage> findByRoomNoOrderByCreatedAtDesc(Long roomNo);

    // 무한 스크롤 (cursor 기준)
    List<ChatMessage> findByRoomNoAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long roomNo,
            LocalDateTime cursor
    );
}