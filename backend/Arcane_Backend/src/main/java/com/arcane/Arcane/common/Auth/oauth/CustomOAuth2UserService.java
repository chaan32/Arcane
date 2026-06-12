package com.arcane.Arcane.common.Auth.oauth;

import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    public static final String OAUTH_LINK_LOGIN_ID_SESSION_KEY = "ARCANE_OAUTH_LINK_LOGIN_ID";
    public static final String OAUTH_LINK_PROVIDER_SESSION_KEY = "ARCANE_OAUTH_LINK_PROVIDER";

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Spring 기본 구현으로 Google/Naver 사용자 정보를 받아온다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 어떤 제공자로 로그인했는지 확인한다. 값은 google/naver 중 하나다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. provider마다 다른 응답 구조를 프로젝트 공통 구조로 변환한다.
        OAuthUserInfo userInfo = OAuthUserInfo.from(registrationId, oAuth2User.getAttributes());
        if (userInfo.getProviderId() == null || userInfo.getProviderId().isBlank()) {
            // providerId는 OAuth 유저를 구분하는 핵심 값이다.
            // 이 값이 없으면 중복 유저를 막을 수 없어서 로그인 실패로 처리한다.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info", "OAuth providerId is missing.", null)
            );
        }

        // 4. users 테이블에서 OAuth 계정을 찾고, 없으면 새로 저장한다.
        // DB 제약 조건 오류 같은 일반 RuntimeException이 그대로 밖으로 나가면
        // Spring 기본 OAuth 로그인 페이지가 노출될 수 있으므로 OAuth 실패로 감싸서 실패 핸들러로 보낸다.
        User user;
        try {
            String linkLoginId = getOAuthLinkLoginId(userInfo.getProvider());
            if (linkLoginId != null) {
                // /me에서 "소셜 계정 연동"을 시작한 경우다.
                // 이때는 email 자동 매칭을 쓰지 않고 현재 로그인 유저에게 providerId를 직접 붙인다.
                user = userService.linkOAuthAccount(
                        linkLoginId,
                        userInfo.getProvider(),
                        userInfo.getProviderId(),
                        userInfo.getEmail(),
                        userInfo.getNickName(),
                        userInfo.getProfileImage()
                );
                clearOAuthLinkIntent();
            } else {
                user = userService.upsertOAuthUser(
                        userInfo.getProvider(),
                        userInfo.getProviderId(),
                        userInfo.getEmail(),
                        userInfo.getNickName(),
                        userInfo.getProfileImage()
                );
            }
        } catch (RuntimeException e) {
            clearOAuthLinkIntent();
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth_user_save_failed", "Failed to save OAuth user.", null),
                    e
            );
        }

        // 5. 성공 핸들러에서 JWT를 만들 수 있도록 DB User를 principal에 담아 반환한다.
        return new ArcaneOAuth2User(user, userInfo.getProvider(), oAuth2User.getAttributes());
    }

    private String getOAuthLinkLoginId(com.arcane.Arcane.web.User.domain.OAuthProvider provider) {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return null;
        }

        Object expectedProvider = session.getAttribute(OAUTH_LINK_PROVIDER_SESSION_KEY);
        Object loginId = session.getAttribute(OAUTH_LINK_LOGIN_ID_SESSION_KEY);
        if (!provider.name().equals(expectedProvider) || !(loginId instanceof String value) || value.isBlank()) {
            return null;
        }
        return value;
    }

    private void clearOAuthLinkIntent() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return;
        }
        session.removeAttribute(OAUTH_LINK_LOGIN_ID_SESSION_KEY);
        session.removeAttribute(OAUTH_LINK_PROVIDER_SESSION_KEY);
    }

    private HttpSession getCurrentSession() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest().getSession(false);
    }
}
