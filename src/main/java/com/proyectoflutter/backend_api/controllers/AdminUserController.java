package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.User;
import com.proyectoflutter.backend_api.payload.request.SignupRequest;
import com.proyectoflutter.backend_api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthController authController;
    private final UserRepository userRepository;

    public AdminUserController(AuthController authController, UserRepository userRepository) {
        this.authController = authController;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createUserWithRole(@Valid @RequestBody SignupRequest signupRequest) {
        return authController.registerUserInternal(signupRequest, true);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(UserSummary::from)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;

        if (currentUsername != null && currentUsername.equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own user");
        }

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    public record UserSummary(Long id, String username, String email, List<String> roles) {
        static UserSummary from(User user) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toList())
            );
        }
    }
}