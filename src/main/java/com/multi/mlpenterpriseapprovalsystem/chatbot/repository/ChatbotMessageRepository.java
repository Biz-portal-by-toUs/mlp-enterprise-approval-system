package com.multi.mlpenterpriseapprovalsystem.chatbot.repository;

import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotMessage;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageRole;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * chatbot message 몽고db 레포지토리
 *
 * @author : 김승기
 * @filename : ChatbotMessageRepository
 * @since : 2026. 1. 1. 목요일
 */
public interface ChatbotMessageRepository extends MongoRepository<ChatbotMessage, String> {


    List<ChatbotMessage> findByEmpId(String empId, Pageable pageable);

    /**
     *커서 기반 무한스크롤
     */
    @Query("""
    {
      'empId': ?0,
      '$or': [
        { 'createdAt': { '$lt': ?1 } },
        { '$and': [
            { 'createdAt': ?1 },
            { '_id': { '$lt': ?2 } }
        ]}
      ]
    }
    """)
    List<ChatbotMessage> findBeforeCursor(String empId, LocalDateTime beforeAt, String beforeId, Pageable pageable);

    // 재연결 시 STREAMING 중인 답변 복구용
    List<ChatbotMessage> findByEmpIdAndRoleAndStatusOrderByUpdatedAtDesc(
            String empId, MessageRole role, MessageStatus status, Pageable pageable
    );
}