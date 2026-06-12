package com.arcane.Arcane.web.User.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_accounts_provider_provider_id", columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(name = "uk_oauth_accounts_user_provider", columnNames = {"user_id", "provider"})
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_image", length = 1000)
    private String profileImage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static OAuthAccount of(User user, OAuthProvider provider, String providerId, String email, String profileImage) {
        if (user == null) {
            throw new IllegalArgumentException("OAuth account user is required.");
        }
        if (provider == null || provider == OAuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth provider must be GOOGLE or NAVER.");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("OAuth providerId is required.");
        }

        return OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .profileImage(profileImage)
                .build();
    }

    public void updateProfile(String email, String profileImage) {
        this.email = email;
        this.profileImage = profileImage;
    }

    public void reassignUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("OAuth account user is required.");
        }
        this.user = user;
    }
}
