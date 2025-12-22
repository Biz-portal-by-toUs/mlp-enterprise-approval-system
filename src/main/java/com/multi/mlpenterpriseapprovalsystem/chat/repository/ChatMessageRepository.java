package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅 컬렉션 접근 repository
 *
 * @author : 김승기
 * @filename : ChatMessageRepository
 * @since : 2025. 12. 16. 화요일
 */
public interface ChatMessageRepository
        extends MongoRepository<ChatMessage, String> {

    Optional<ChatMessage> findTopByRoomNoOrderByCreatedAtDesc(Long roomNo);

    List<ChatMessage> findByRoomNoAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long roomNo,
            LocalDateTime joinedAt
    );
    List<ChatMessage> findByRoomNoAndCreatedAtLessThanAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long roomNo,
            LocalDateTime cursor,
            LocalDateTime joinedAt
    );
}