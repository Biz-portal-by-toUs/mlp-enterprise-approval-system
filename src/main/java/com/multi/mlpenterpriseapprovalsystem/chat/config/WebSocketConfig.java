package com.multi.mlpenterpriseapprovalsystem.chat.config;

import com.multi.mlpenterpriseapprovalsystem.chat.interceptor.CustomHandshakeInterceptor;
import com.multi.mlpenterpriseapprovalsystem.chat.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 통신망 구축 설정 (메세지가 다니는 길)
 * 
 * @filename    : WebSocketConfig
 * @author      : 김승기
 * @since       : 2025. 12. 17. 수요일
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");         // 구독
        registry.setApplicationDestinationPrefixes("/pub"); // 발행(서버로 들어옴)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new CustomHandshakeInterceptor()) // 인터셉터 추가
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }


}