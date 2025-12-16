package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ChatMessageRepository
 * @since : 2025. 12. 16. 화요일
 */
public interface ChatMessageRepository
        extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(
            String roomId, Pageable pageable);
}