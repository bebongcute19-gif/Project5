package com.omnichannel.identity.config;

import com.omnichannel.identity.entity.Address;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.repository.AddressRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Seed data runner for Identity Service.
 * Creates initial users, roles, and addresses for testing.
 * 
 * Run with: mvn spring-boot:run -Dspring-boot.run.arguments="--seed-data"
 * Or set environment variable: SEED_DATA=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentitySeedDataRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String seedDataEnv = System.getenv("SEED_DATA");
        if (seedDataEnv == null || !seedDataEnv.equalsIgnoreCase("true")) {
            log.debug("Seed data disabled. Set SEED_DATA=true environment variable to enable.");
            return;
        }

        log.info("Starting Identity Service Seed Data initialization...");

        try {
            // Create roles if they don't exist
            createRoles();

            // Create users
            createUsers();

            // Create addresses
            createAddresses();

            log.info("Seed data initialization completed successfully!");
        } catch (Exception e) {
            log.error("Error initializing seed data: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void createRoles() {
        // Roles should already be created by Flyway migration
        // This is just a verification
        long roleCount = roleRepository.count();
        if (roleCount == 0) {
            log.info("Creating default roles...");
            roleRepository.save(Role.builder()
                    .name("ADMIN")
                    .description("System Administrator")
                    .build());
            roleRepository.save(Role.builder()
                    .name("CUSTOMER")
                    .description("Customer")
                    .build());
            log.info("Roles created successfully");
        } else {
            log.debug("Roles already exist. Skipping role creation.");
        }
    }

    private void createUsers() {
        log.info("Creating test users...");

        // Get roles
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));

        // Admin user
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .username("admin")
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .phone("+84912345678")
                    .status("ACTIVE")
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(adminRole, customerRole)))
                    .build();
            userRepository.save(admin);
            log.info("Admin user created: {}", admin.getUsername());
        } else {
            log.debug("Admin user already exists");
        }

        // Test users
        String[] users = {
                "user1", "user2", "user3", "user4", "user5"
        };
        String[] emails = {
                "user1@example.com", "user2@example.com", "user3@example.com",
                "user4@example.com", "user5@example.com"
        };
        String[] phones = {
                "+84912345671", "+84912345672", "+84912345673",
                "+84912345674", "+84912345675"
        };

        for (int i = 0; i < users.length; i++) {
            if (!userRepository.existsByUsername(users[i])) {
                UUID userId = UUID.fromString(
                        String.format("11111111-1111-1111-1111-11111111111%d", i)
                );
                User user = User.builder()
                        .id(userId)
                        .username(users[i])
                        .email(emails[i])
                        .passwordHash(passwordEncoder.encode("password123"))
                        .fullName("Test User " + (i + 1))
                        .phone(phones[i])
                        .status("ACTIVE")
                        .enabled(true)
                        .roles(new HashSet<>(Set.of(customerRole)))
                        .build();
                userRepository.save(user);
                log.info("User created: {} ({})", user.getUsername(), user.getId());
            } else {
                log.debug("User {} already exists", users[i]);
            }
        }

        log.info("Test users creation completed");
    }

    private void createAddresses() {
        log.info("Creating test addresses...");

        // Get the first test user (user1)
        User user1 = userRepository.findByUsername("user1")
                .orElseThrow(() -> new RuntimeException("user1 not found"));

        // Create primary address for user1
        if (addressRepository.findByUserId(user1.getId()).isEmpty()) {
            Address address1 = Address.builder()
                    .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .userId(user1.getId())
                    .recipientName("Test User 1")
                    .phone("+84912345671")
                    .streetAddress("123 Le Loi Street")
                    .ward("Ward 1")
                    .district("District 1")
                    .city("Ho Chi Minh City")
                    .isDefault(true)
                    .build();
            addressRepository.save(address1);
            log.info("Primary address created for user1");

            // Create secondary address for user1
            Address address2 = Address.builder()
                    .id(UUID.fromString("22222222-2222-2222-2222-222222222223"))
                    .userId(user1.getId())
                    .recipientName("Test User 1 - Office")
                    .phone("+84912345671")
                    .streetAddress("456 Nguyen Hue Boulevard")
                    .ward("Ward 2")
                    .district("District 1")
                    .city("Ho Chi Minh City")
                    .isDefault(false)
                    .build();
            addressRepository.save(address2);
            log.info("Secondary address created for user1");
        } else {
            log.debug("Addresses for user1 already exist");
        }

        // Create addresses for other test users
        for (int i = 2; i <= 5; i++) {
            final int userIndex = i;
            User user = userRepository.findByUsername("user" + userIndex)
                    .orElseThrow(() -> new RuntimeException("user" + userIndex + " not found"));

            if (addressRepository.findByUserId(user.getId()).isEmpty()) {
                Address address = Address.builder()
                        .id(UUID.fromString(
                                String.format("22222222-2222-2222-2222-22222222222%d", userIndex)
                        ))
                        .userId(user.getId())
                        .recipientName("Test User " + userIndex)
                        .phone("+8491234567" + userIndex)
                        .streetAddress(String.format("%d Test Street", userIndex * 100))
                        .ward("Ward " + userIndex)
                        .district("District 1")
                        .city("Ho Chi Minh City")
                        .isDefault(true)
                        .build();
                addressRepository.save(address);
                log.info("Default address created for user{}", userIndex);
            }
        }

        log.info("Test addresses creation completed");
    }
}
