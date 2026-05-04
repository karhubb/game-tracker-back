package com.proyectoflutter.backend_api.controllers;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoflutter.backend_api.payload.response.ReactionTypeResponseDTO;
import com.proyectoflutter.backend_api.repository.ReactionRepository;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionRepository reactionRepository;

    public ReactionController(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    @GetMapping
    public List<ReactionTypeResponseDTO> getReactionTypes() {
        return reactionRepository.findAll().stream()
                .sorted(Comparator.comparingLong(r -> r.getId() == null ? Long.MAX_VALUE : r.getId()))
                .map(ReactionTypeResponseDTO::new)
                .toList();
    }
}
