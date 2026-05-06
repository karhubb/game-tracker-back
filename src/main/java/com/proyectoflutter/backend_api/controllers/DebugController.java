package com.proyectoflutter.backend_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.proyectoflutter.backend_api.security.services.CurrentUserService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * Debug endpoint: Shows current user info and roles.
     * This helps diagnose authentication and role issues.
     */
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            result.put("authenticated", auth != null && auth.isAuthenticated());
            result.put("principal", auth != null ? auth.getPrincipal() : null);
            result.put("username", currentUserService.getUsername().orElse("NOT_AUTHENTICATED"));
            result.put("authorities", auth != null ? auth.getAuthorities() : null);
            result.put("isAdmin", currentUserService.hasRole("ROLE_ADMIN"));
            result.put("isModerator", currentUserService.hasRole("ROLE_MODERATOR"));
            result.put("allRoles", auth != null ? 
                    auth.getAuthorities().stream().map(a -> a.getAuthority()).toList() : 
                    null);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
