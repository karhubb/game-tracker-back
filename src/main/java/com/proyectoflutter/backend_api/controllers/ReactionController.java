package com.proyectoflutter.backend_api.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoflutter.backend_api.payload.response.ReactionTypeResponseDTO;
import com.proyectoflutter.backend_api.services.ReactionService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    // Controlador fino: expone DTOs de catálogo sin mezclar la consulta HTTP
    // con la lógica de acceso a datos, manteniendo el mismo estilo de los demás endpoints.
    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @GetMapping
    public java.util.List<ReactionTypeResponseDTO> getReactionTypes() {
        return reactionService.getReactionTypes();
    }
}
