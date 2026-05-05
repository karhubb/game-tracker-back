package com.proyectoflutter.backend_api.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.proyectoflutter.backend_api.security.jwt.AuthEntryPointJwt;
import com.proyectoflutter.backend_api.security.jwt.AuthTokenFilter;
import com.proyectoflutter.backend_api.security.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
// (securedEnabled = true,
// jsr250Enabled = true,
// prePostEnabled = true) // by default
public class WebSecurityConfig   {
  @Autowired
  UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
      DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
      authProvider.setPasswordEncoder(passwordEncoder());
      return authProvider;
  }
 
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
  
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
      .cors(org.springframework.security.config.Customizer.withDefaults()) 
      .csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  
  // CRITICAL: AuthorizeHttpRequests MUST be configured BEFORE exceptionHandling
  http.authorizeHttpRequests(auth -> {
    // Public endpoints - ALL HTTP methods
    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
    auth.requestMatchers("/").permitAll();
    auth.requestMatchers("/api/health").permitAll();
    
    // Auth endpoints - EXPLICITLY allow all methods
    auth.requestMatchers("/api/auth/signin").permitAll();
    auth.requestMatchers("/api/auth/login").permitAll();
    auth.requestMatchers("/api/auth/signup").permitAll();
    
    // Games endpoints - public read, private write
    auth.requestMatchers(HttpMethod.GET, "/api/juegos").permitAll();
    auth.requestMatchers(HttpMethod.GET, "/api/juegos/**").permitAll();
    auth.requestMatchers(HttpMethod.GET, "/api/reactions").permitAll();
    auth.requestMatchers(HttpMethod.GET, "/api/reactions/**").permitAll();
    auth.requestMatchers(HttpMethod.GET, "/api/notes/reactions/**").permitAll();
    
    // Protected endpoints
    auth.requestMatchers(HttpMethod.POST, "/api/juegos/**").authenticated();
    auth.requestMatchers(HttpMethod.PUT, "/api/juegos/**").authenticated();
    auth.requestMatchers(HttpMethod.DELETE, "/api/juegos/**").authenticated();
    
    // Everything else requires authentication
    auth.anyRequest().authenticated();
  });
  
  // Exception handling AFTER authorization
  http.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler));
  
  http.authenticationProvider(authenticationProvider());
  http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    
  return http.build();
}

      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
      }
}