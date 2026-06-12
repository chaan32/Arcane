package com.arcane.Arcane.common.Auth.oauth;

import com.arcane.Arcane.common.Auth.jwt.JwtUtil;
import com.arcane.Arcane.web.User.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // CustomOAuth2UserService가 반환한 principal이다.
        // 여기 안에 DB에 저장된 User가 들어있다.
        ArcaneOAuth2User principal = (ArcaneOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        // 기존 일반 로그인과 동일한 JWT를 발급한다.
        // 프론트는 이 token을 localStorage 등에 저장하고 Authorization: Bearer ... 형태로 사용하면 된다.
        String token = jwtUtil.createToken(user.getLoginId(), user.getRole());

        // OAuth 로그인 완료 후 프론트 콜백 페이지로 이동한다.
        // token, userId, nickName을 같이 넘겨서 프론트가 바로 로그인 상태를 만들 수 있게 한다.
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("token", token)
                .queryParam("userId", user.getId())
                .queryParam("loginId", user.getLoginId())
                .queryParam("nickName", user.getNickName() == null ? "" : user.getNickName())
                .queryParam("role", user.getRole().name())
                .queryParam("provider", principal.getProvider().name().toLowerCase())
                .queryParam("needsOnboarding", user.isOnboardingRequired())
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
