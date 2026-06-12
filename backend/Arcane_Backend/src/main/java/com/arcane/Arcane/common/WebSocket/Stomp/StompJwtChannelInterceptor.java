package com.arcane.Arcane.common.WebSocket.Stomp;

import com.arcane.Arcane.common.Auth.jwt.JwtUtil;
import com.arcane.Arcane.web.User.domain.Role;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;
    @Value("${jwt.password}")
    private String password;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP Header를 읽기 위한 accessor ( 메소드로 들어온 message를 header를 봄)
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null){ // STOMP 메세지 정보가 없으면 걍 통과
            return message;
        }

        if (accessor.getCommand().equals(StompCommand.CONNECT)){ // Connect 연결을 시도할 때

            // accessor에서 Authorization 가져옴
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")) { // Bearer로 시작하지 않거나, Authorization 객체 없으면 토큰 없음
                throw new IllegalArgumentException("WebSocket 인증 토큰이 없어요!");
            }

            String token = authorization.substring(7); // 앞에 'Bearer ' 제거하고 token 가져옴


            Claims claims = jwtUtil.parseClaims(token); // jwt 파싱 : 만료 / 위조 토큰이라면 제대로 파싱이 안됨

            String passphrase = claims.get("dont", String.class);

            if (!password.equals(passphrase)) {
                throw new IllegalArgumentException("암구호가 올바르지 않습니다.");
            }


            String loginId = claims.get("loginId", String.class); // jwt에서 loginId 추출

            Role role = Role.valueOf(claims.get("role", String.class)); // JWT에서 role을 꺼내 Role enum으로 변환

            // Spring Security가 이해하는 인증 객체
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken( // username, password, authorities를 담는 인증 객체
                            loginId, // principal 값이다. 나중에 Principal.getName()으로 이 loginId가 나옴
                            null, // JWT 인증에서는 비밀번호가 필요 없다함
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.name())) // ROLE_USER 같은 권한을 넣음
                    );

            // Stomp 세션에 인증된 사용자 정보를 심음
            accessor.setUser(authentication);
        }

        return message;
    }
}
