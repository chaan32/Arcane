package com.arcane.Arcane.web.Admin.config;

import com.arcane.Arcane.web.User.domain.Role;
import com.arcane.Arcane.web.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserBootstrap implements ApplicationRunner {
    private static final long ADMIN_USER_ID = 5L;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findById(ADMIN_USER_ID)
                .ifPresentOrElse(user -> {
                    if (user.getRole() == Role.ADMIN) {
                        log.info("Admin user already configured: userId={}", ADMIN_USER_ID);
                        return;
                    }

                    user.updateRole(Role.ADMIN);
                    userRepository.save(user);
                    log.info("Admin user configured: userId={}", ADMIN_USER_ID);
                }, () -> log.warn("Admin user was not found: userId={}", ADMIN_USER_ID));
    }
}
