package com.proyectoflutter.backend_api.config;

import com.proyectoflutter.backend_api.models.ERole;
import com.proyectoflutter.backend_api.models.Role;
import com.proyectoflutter.backend_api.models.User;
import com.proyectoflutter.backend_api.repository.RoleRepository;
import com.proyectoflutter.backend_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${gametracker.app.admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${gametracker.app.admin.username:admin}")
    private String adminUsername;

    @Value("${gametracker.app.admin.email:admin@gametracker.local}")
    private String adminEmail;

    @Value("${gametracker.app.admin.password:Admin123!}")
    private String adminPassword;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRole(ERole.ROLE_USER);
        ensureRole(ERole.ROLE_MODERATOR);
        Role adminRole = ensureRole(ERole.ROLE_ADMIN);

        if (!adminEnabled) {
            logger.info("Admin bootstrap disabled via gametracker.app.admin.enabled=false");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)) {
            logger.info("Admin user '{}' already exists, skipping bootstrap", adminUsername);
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            logger.warn("Admin bootstrap skipped because email '{}' is already in use", adminEmail);
            return;
        }

        User adminUser = new User(
                adminUsername,
                adminEmail,
                passwordEncoder.encode(adminPassword)
        );
        adminUser.setRoles(Set.of(adminRole));

        userRepository.save(adminUser);
        logger.info("Default admin user '{}' created", adminUsername);
    }

    private Role ensureRole(ERole roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }
}