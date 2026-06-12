package com.arcane.Arcane.common.Auth.oauth;

import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.domain.OAuthProvider;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class ArcaneOAuth2User implements OAuth2User {
    private final User user;
    private final OAuthProvider provider;
    private final Map<String, Object> attributes;

    public ArcaneOAuth2User(User user, OAuthProvider provider, Map<String, Object> attributes) {
        this.user = user;
        this.provider = provider;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        // OAuth 제공자가 내려준 원본 attributes를 유지한다.
        // 디버깅이나 추가 프로필 필드가 필요할 때 여기서 확인할 수 있다.
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기존 JWT 필터와 동일하게 ROLE_USER, ROLE_ADMIN 형태로 권한을 맞춘다.
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        // Spring Security가 principal 이름으로 사용할 값이다.
        // 프로젝트의 기존 인증 기준이 loginId라서 OAuth 유저도 내부 loginId를 사용한다.
        return user.getLoginId();
    }
}
