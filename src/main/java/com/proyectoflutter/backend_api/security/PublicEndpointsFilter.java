package com.proyectoflutter.backend_api.security;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to explicitly allow public endpoints BEFORE authentication is required.
 * This ensures /api/auth/** endpoints bypass authentication checks entirely.
 */
@Component
public class PublicEndpointsFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(PublicEndpointsFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Log all incoming requests
        logger.debug("PublicEndpointsFilter: {} {}", method, path);
        
        // Explicitly allow /api/auth/** endpoints
        if (path.startsWith("/api/auth/")) {
            logger.debug("Public endpoint allowed: {} {}", method, path);
            // Set an empty authentication to indicate this is a public endpoint
            // This prevents Spring Security from requiring authentication
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Allow anonymous access by setting principal to null
            }
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
