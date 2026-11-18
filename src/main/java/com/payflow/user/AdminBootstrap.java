package com.payflow.user;

import com.payflow.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the admin account on startup from the ADMIN_EMAIL and ADMIN_PASSWORD
 * environment variables. Public registration can never create an admin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;

    @Value("${payflow.admin.email:}")
    private String adminEmail;

    @Value("${payflow.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set; skipping admin bootstrap");
            return;
        }
        if (adminPassword.length() < 12) {
            log.warn("ADMIN_PASSWORD is shorter than 12 characters; skipping admin bootstrap");
            return;
        }

        String email = adminEmail.trim();
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (user.getRole() != User.Role.ADMIN) {
                user.setRole(User.Role.ADMIN);
                userRepository.save(user);
                log.info("Promoted existing user {} to ADMIN", email);
            }
        }, () -> {
            User admin = userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .build());
            walletService.createWallet(admin);
            log.info("Created admin account {}", email);
        });
    }
}
