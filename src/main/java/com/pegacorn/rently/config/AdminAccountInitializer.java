package com.pegacorn.rently.config;

import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.entity.UserRole;
import com.pegacorn.rently.repository.UserRepository;
import com.pegacorn.rently.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_PHONE = "0989762205";
    private static final String ADMIN_PASSWORD = "123456";
    private static final String ADMIN_NAME = "System Admin";

    @Override
    public void run(String... args) {
        if (userRepository.findByPhone(ADMIN_PHONE).isEmpty()) {
            log.info("Creating default admin account with phone: {}", ADMIN_PHONE);

            String userId = UUID.randomUUID().toString();

            // Create user
            User admin = User.builder()
                    .id(userId)
                    .phone(ADMIN_PHONE)
                    .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                    .fullName(ADMIN_NAME)
                    .status(User.UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);

            // Assign SYSTEM_ADMIN role
            UserRole role = UserRole.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .role(User.Role.SYSTEM_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRoleRepository.save(role);

            log.info("Default admin account created successfully");
        } else {
            log.debug("Admin account already exists");
        }
    }
}
