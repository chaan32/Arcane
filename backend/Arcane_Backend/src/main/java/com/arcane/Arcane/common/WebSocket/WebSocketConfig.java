package com.arcane.Arcane.common.WebSocket;

import com.arcane.Arcane.common.WebSocket.Stomp.StompJwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOriginPatterns;

    // WebSocket 접속 주소를 등록하는 메소드
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws/chat") // 웹소켓 주소를 /ws/chat으로
                .setAllowedOriginPatterns(parseAllowedOriginPatterns().toArray(String[]::new)); // 프론트의 주소
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue"); // 구독할 수 있는 broker prefix
        registry.setApplicationDestinationPrefixes("/pub"); // 클라이언트가 메세지를 보낼 때 사용하는 거
        registry.setUserDestinationPrefix("/user"); // 특정 유저에게만 메세지를 보낼 때 사용
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor); // 우리가 만든 인터셉터를 등록함으로써 통해서 JWT 인증 수행을 하도록 함
    }

    private List<String> parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
