package com.arcane.Arcane.common.Auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
    @Value("${app.oauth2.failure-redirect-uri:http://localhost:3000/oauth/callback}")
    private String frontendFailureRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // OAuth 인증 실패도 프론트 콜백 페이지로 넘긴다.
        // 프론트는 error 파라미터가 있으면 실패 화면이나 토스트를 보여주면 된다.
        String errorCode = "oauth_failed";
        String errorMessage = exception.getMessage();

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            errorCode = oAuth2Exception.getError().getErrorCode();
            if (oAuth2Exception.getCause() != null && oAuth2Exception.getCause().getMessage() != null) {
                errorMessage = oAuth2Exception.getCause().getMessage();
            }
        }

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendFailureRedirectUri)
                .queryParam("error", errorCode)
                .queryParam("message", errorMessage == null ? "OAuth 인증에 실패했습니다." : errorMessage)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
