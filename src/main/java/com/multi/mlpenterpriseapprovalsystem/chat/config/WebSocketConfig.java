package com.multi.mlpenterpriseapprovalsystem.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 웹소켓 연결 및 메세지 브로커 config
 * 
 * @filename    : WebSocketConfig
 * @author      : 김승기
 * @since       : 2025. 12. 17. 수요일
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 클라이언트가 최초로 WebSocket 연결할 Endpoint
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 클라이언트 → 서버
        registry.setApplicationDestinationPrefixes("/pub");

        // 서버 → 클라이언트
        registry.enableSimpleBroker("/sub");
    }
}