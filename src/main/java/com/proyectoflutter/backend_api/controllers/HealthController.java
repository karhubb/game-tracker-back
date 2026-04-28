package com.proyectoflutter.backend_api.controllers;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/")
  public ResponseEntity<Void> root() {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/api/auth/login"))
        .build();
  }

  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }
}
