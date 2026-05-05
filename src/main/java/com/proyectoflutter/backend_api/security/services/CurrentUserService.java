package com.proyectoflutter.backend_api.security.services;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * DESIGN PATTERN: Facade Pattern (Structural)
 * 
 * Purpose:
 * Provide a unified, testable interface to Spring Security's SecurityContextHolder,
 * centralizing how controllers access authentication context and role information.
 * 
 * Why This Pattern?
 * - SecurityContextHolder static calls scattered across 3+ controllers
 * - Each controller duplicates the same authentication extraction and validation logic
 * - Security rules can change in ONE place instead of N places
 * - Makes unit testing easier: Mock CurrentUserService instead of static context
 * - Follows Facade principle: Hide complex Spring Security infrastructure
 * 
 * Usage Example (Before):
 *   String username = SecurityContextHolder.getContext().getAuthentication().getName();
 *   // Repeated in GameController, AdminUserController, NoteReactionController
 * 
 * Usage Example (After):
 *   String username = currentUserService.requireUsername();
 *   // Cleaner, testable, DRY
 * 
 * Benefits:
 * - Controllers focus on business logic, not security plumbing
 * - Optional vs. Required pattern: getUsername() vs. requireUsername()
 * - Single point of maintenance
 */
@Service
public class CurrentUserService {

    /**
     * Get the username of the currently authenticated user.
     * 
     * @return Optional containing username if authenticated and valid; empty otherwise
     */
    public Optional<String> getUsername() {
        Authentication authentication = authentication();
        if (!isAuthenticated(authentication)) {
            return Optional.empty();
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return Optional.empty();
        }

        return Optional.of(username);
    }

    /**
     * Get the username of the currently authenticated user, or throw UNAUTHORIZED.
     * 
     * Use this in endpoints that REQUIRE authentication to proceed.
     * 
     * @return The username string
     * @throws ResponseStatusException with HttpStatus.UNAUTHORIZED if not authenticated
     */
    public String requireUsername() {
        return getUsername()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
    }

    /**
     * Check if the current user has a specific role.
     * 
     * @param roleName The role name to check (e.g., "ROLE_ADMIN", "ROLE_MODERATOR")
     * @return true if user has the role; false if not authenticated or lacks role
     */
    public boolean hasRole(String roleName) {
        Authentication authentication = authentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }

    /**
     * Check if a user is currently authenticated.
     * 
     * @return true if authentication is valid and not anonymous; false otherwise
     */
    public boolean isAuthenticated() {
        return isAuthenticated(authentication());
    }

    /**
     * Private helper: Extract Authentication from SecurityContext.
     */
    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Private helper: Check if an Authentication object is valid and not anonymous.
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}

