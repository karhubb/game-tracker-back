package com.proyectoflutter.backend_api.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.proyectoflutter.backend_api.models.ERole;
import com.proyectoflutter.backend_api.models.Role;
import com.proyectoflutter.backend_api.models.User;
import com.proyectoflutter.backend_api.payload.request.LoginRequest;
import com.proyectoflutter.backend_api.payload.request.SignupRequest;
import com.proyectoflutter.backend_api.payload.response.JwtResponse;
import com.proyectoflutter.backend_api.payload.response.MessageResponse;
import com.proyectoflutter.backend_api.repository.RoleRepository;
import com.proyectoflutter.backend_api.repository.UserRepository;
import com.proyectoflutter.backend_api.security.jwt.JwtUtils;
import com.proyectoflutter.backend_api.security.services.UserDetailsImpl;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @GetMapping("/login")
  public ResponseEntity<?> loginEntryPoint() {
    return ResponseEntity.ok(new MessageResponse("Use POST /api/auth/login with username and password to authenticate."));
  }

  @PostMapping({"/signin", "/login"})
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity
        .ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    return registerUserInternal(signUpRequest, false);
  }

  ResponseEntity<?> registerUserInternal(SignupRequest signUpRequest, boolean allowRoleSelection) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
        encoder.encode(signUpRequest.getPassword()));

    Set<Role> roles = resolveRoles(signUpRequest.getRoles(), allowRoleSelection);

    user.setRoles(roles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }

  private Set<Role> resolveRoles(Set<String> requestedRoles, boolean allowRoleSelection) {
    Set<Role> roles = new HashSet<>();

    if (!allowRoleSelection || requestedRoles == null || requestedRoles.isEmpty()) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
      return roles;
    }

    requestedRoles.forEach(role -> {
      switch (role.toLowerCase()) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);
          break;
        case "mod":
        case "moderator":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);
          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
      }
    });

    if (roles.isEmpty()) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    }

    return roles;
  }
}