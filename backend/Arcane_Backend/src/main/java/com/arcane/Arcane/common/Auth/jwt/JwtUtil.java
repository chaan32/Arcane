package com.arcane.Arcane.common.Auth.jwt;

import com.arcane.Arcane.web.User.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j(topic = "JwtUtil")
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // JWT 생성 v1
    /*
    public String createToken(String loginId){
        return Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

    }*/

    // JWT에서 사용자 ID 추출 v1
    /*
    public String extractLoginId(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }*/


    // JWT 생성 v2
    public String createToken(String loginId, Role role){
        Claims claims = Jwts.claims();
        claims.put("role", role); // role 권한 정보를 추가하기
        claims.put("loginId", loginId);
        claims.put("dont", "again");

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT에서 사용자 ID 추출 v2
    public String extractLoginId(String token){
        return parseClaims(token).get("loginId", String.class);
    }

    // JWT 토큰에서 role을 추출하는 메소드
    public Role extractRole(String token){
        String roleStr = parseClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰 유효성 검사
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT 토큰 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token, 유효하지 않은 JWT 토큰 입니다.");
        } catch (SignatureException e) {
            log.error("Invalid JWT signature, 유효하지 않은 JWT 서명 입니다.");
        }
        return false;
    }
}
