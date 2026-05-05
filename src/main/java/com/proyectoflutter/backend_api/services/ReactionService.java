package com.proyectoflutter.backend_api.services;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoflutter.backend_api.models.Reaction;
import com.proyectoflutter.backend_api.payload.response.ReactionTypeResponseDTO;
import com.proyectoflutter.backend_api.repository.ReactionRepository;

@Service
public class ReactionService {

    // Servicio de aplicación: concentra el acceso a ReactionRepository para que
    // el controller quede como adaptador HTTP delgado, siguiendo el mismo patrón
    // de separación que el resto del backend.
    private final ReactionRepository reactionRepository;

    public ReactionService(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    @Transactional(readOnly = true)
    public List<ReactionTypeResponseDTO> getReactionTypes() {
        return reactionRepository.findAll().stream()
                .sorted(Comparator.comparingLong(r -> r.getId() == null ? Long.MAX_VALUE : r.getId()))
                .map(ReactionTypeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Reaction> getReactionById(Long reactionId) {
        return reactionRepository.findById(reactionId);
    }
}