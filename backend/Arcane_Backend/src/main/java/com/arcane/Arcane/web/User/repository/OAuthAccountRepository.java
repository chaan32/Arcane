package com.arcane.Arcane.web.User.repository;

import com.arcane.Arcane.web.User.domain.OAuthAccount;
import com.arcane.Arcane.web.User.domain.OAuthProvider;
import com.arcane.Arcane.web.User.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);
    Optional<OAuthAccount> findByUserAndProvider(User user, OAuthProvider provider);
    List<OAuthAccount> findAllByUser(User user);
}
