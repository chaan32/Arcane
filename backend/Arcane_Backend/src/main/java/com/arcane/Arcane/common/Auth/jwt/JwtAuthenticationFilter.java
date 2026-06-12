package com.arcane.Arcane.common.Auth.jwt;

import com.arcane.Arcane.web.User.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // 7자리 끊고

            try {
                Claims claims = jwtUtil.parseClaims(token);
                // 1) loginId, role 가져오기
                String loginId = claims.get("loginId", String.class);
                Role role = Role.valueOf(claims.get("role", String.class));

                // 2) 역할 정보를 Spring security에서 사용하는 GrantedAuthority 형식으로 만듦
                List<SimpleGrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+role.name()));

                // 3) 인증 객체를 생성할 때 위에서 만든 authorities 넣어주기
                Authentication authentication = new UsernamePasswordAuthenticationToken(loginId, null, authorities);

                // 4) 보안 컨텍스트에 새로운 인증 정보 설정하기
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                writeUnauthorizedResponse(response, "TOKEN_EXPIRED", "로그인 시간이 만료되었습니다. 다시 로그인해주세요.");
                return;
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                writeUnauthorizedResponse(response, "INVALID_TOKEN", "유효하지 않은 인증 토큰입니다. 다시 로그인해주세요.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"code":"%s","message":"%s"}
                """.formatted(code, message));
    }
}
