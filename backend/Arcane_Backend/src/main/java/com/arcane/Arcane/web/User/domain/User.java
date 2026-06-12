package com.arcane.Arcane.web.User.domain;


import com.arcane.Arcane.web.User.dto.request.UserSignUpRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private static final int LOGIN_ID_MAX_LENGTH = 255;
    private static final int NICK_NAME_MAX_LENGTH = 20;
    public static final String OAUTH_PASSWORD_PLACEHOLDER = "{oauth}";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name="login_id", length = LOGIN_ID_MAX_LENGTH, nullable = false)
    private String loginId;

    @Column(name="login_pw", length = 255)
    private String loginPw;

    @Column(name="game_name", length = 100)
    private String gameName;

    @Column(name="trimmed_game_name", length = 100)
    private String trimmedGameName;

    @Column(name="nick_name",length = 20)
    private String nickName;

    @Column(name="tag_line", length = 20)
    private String tagLine;

    @Column(name = "puuid", length = 100)
    private String puuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20, columnDefinition = "varchar(20) default 'LOCAL'")
    private OAuthProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_image", length = 1000)
    private String profileImage;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @PrePersist
    protected void onCreate() {
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.provider == null) {
            this.provider = OAuthProvider.LOCAL;
        }
        if (this.onboardingCompleted == null) {
            this.onboardingCompleted = hasText(this.nickName);
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.provider == null) {
            this.provider = OAuthProvider.LOCAL;
        }
        if (this.onboardingCompleted == null) {
            this.onboardingCompleted = hasText(this.nickName);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public static User of (UserSignUpRequestDto dto, PasswordEncoder encoder) {
        String trimmedGameName = dto.getGameName().replace(" ", ""); // 공백 제거 버전

        return User.builder()
                .loginId(dto.getLoginId())
                .loginPw(encoder.encode(dto.getLoginPw()))
                .gameName(dto.getGameName())
                .trimmedGameName(trimmedGameName)
                .puuid(dto.getPuuid())
                .tagLine(dto.getTagLine())
                .nickName(dto.getNickName())
                .provider(OAuthProvider.LOCAL)
                .onboardingCompleted(true)
                .role(Role.USER) // 기본 역할을 USER로 만들기
                .build();
    }

    public static User oauth(OAuthProvider provider, String providerId, String email, String nickName, String profileImage) {
        if (provider == null || provider == OAuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth provider must be GOOGLE or NAVER.");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("OAuth providerId is required.");
        }

        String loginId = buildOAuthLoginId(provider, providerId, email);
        String fallbackNickName = provider.name().toLowerCase() + "_" + providerId;

        return User.builder()
                .loginId(loginId)
                .loginPw(OAUTH_PASSWORD_PLACEHOLDER)
                .nickName(null)
                .provider(OAuthProvider.LOCAL)
                .providerId(null)
                .email(email)
                .profileImage(profileImage)
                .onboardingCompleted(false)
                .role(Role.USER)
                .build();
    }

    public void updateOAuthProfile(String email, String nickName, String profileImage) {
        this.email = email;
        this.profileImage = profileImage;
    }

    public void updateRole(Role role) {
        this.role = role == null ? Role.USER : role;
    }

    public void completeOnboarding(String nickName) {
        this.nickName = normalizeNickName(nickName, this.loginId);
        this.onboardingCompleted = true;
    }

    public boolean isOnboardingRequired() {
        return !hasText(this.nickName);
    }

    private static String normalizeNickName(String nickName, String fallbackNickName) {
        String normalized = nickName == null || nickName.isBlank() ? fallbackNickName : nickName.trim();
        if (normalized.length() <= NICK_NAME_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, NICK_NAME_MAX_LENGTH);
    }

    private static String buildOAuthLoginId(OAuthProvider provider, String providerId, String email) {
        String loginId = hasText(email)
                ? "oauth_" + sha256(email.trim().toLowerCase())
                : provider.name().toLowerCase() + "_" + providerId;
        if (loginId.length() <= LOGIN_ID_MAX_LENGTH) {
            return loginId;
        }
        return provider.name().toLowerCase() + "_" + sha256(providerId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
