package com.arcane.Arcane.common.Auth.oauth;

import com.arcane.Arcane.web.User.domain.OAuthProvider;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthUserInfo {
    private final OAuthProvider provider;
    private final String providerId;
    private final String email;
    private final String nickName;
    private final String profileImage;

    private OAuthUserInfo(OAuthProvider provider, String providerId, String email, String nickName, String profileImage) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickName = nickName;
        this.profileImage = profileImage;
    }

    public static OAuthUserInfo from(String registrationId, Map<String, Object> attributes) {
        // registrationId는 application.yml의 registration 이름이다.
        // google 로그인이면 "google", naver 로그인이면 "naver"가 들어온다.
        return switch (registrationId.toLowerCase()) {
            case "google" -> fromGoogle(attributes);
            case "naver" -> fromNaver(attributes);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }

    private static OAuthUserInfo fromGoogle(Map<String, Object> attributes) {
        // Google OAuth 응답의 고유 식별자는 sub다.
        // 같은 Google 계정이면 sub가 항상 동일하므로 providerId로 사용한다.
        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                stringOrNull(attributes.get("sub")),
                stringOrNull(attributes.get("email")),
                stringOrNull(attributes.get("name")),
                stringOrNull(attributes.get("picture"))
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuthUserInfo fromNaver(Map<String, Object> attributes) {
        // Naver는 사용자 정보가 최상위가 아니라 response 객체 안에 들어온다.
        // 예: { resultcode, message, response: { id, email, name, profile_image } }
        Object response = attributes.get("response");
        if (!(response instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Naver OAuth response is missing.");
        }

        Map<String, Object> responseAttributes = (Map<String, Object>) response;
        return new OAuthUserInfo(
                OAuthProvider.NAVER,
                stringOrNull(responseAttributes.get("id")),
                stringOrNull(responseAttributes.get("email")),
                stringOrNull(responseAttributes.get("name")),
                stringOrNull(responseAttributes.get("profile_image"))
        );
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
