package com.proyectoflutter.backend_api.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoflutter.backend_api.payload.request.NoteReactionRequest;
import com.proyectoflutter.backend_api.payload.response.MessageResponse;
import com.proyectoflutter.backend_api.payload.response.NoteReactionResponseDTO;
import com.proyectoflutter.backend_api.payload.response.NoteReactionSummaryDTO;
import com.proyectoflutter.backend_api.security.services.CurrentUserService;
import com.proyectoflutter.backend_api.services.NoteReactionService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notes/reactions")
public class NoteReactionController {

    private final NoteReactionService noteReactionService;
    private final CurrentUserService currentUserService;

    public NoteReactionController(NoteReactionService noteReactionService, CurrentUserService currentUserService) {
        this.noteReactionService = noteReactionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/games/{gameId}/notes/{noteIndex}")
    public ResponseEntity<NoteReactionResponseDTO> reactToNote(
            @PathVariable Long gameId,
            @PathVariable Integer noteIndex,
            @Valid @RequestBody NoteReactionRequest request
    ) {
        NoteReactionResponseDTO response = noteReactionService.createOrUpdateReaction(
                currentUserService.requireUsername(),
                gameId,
                noteIndex,
                request.getReactionId()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/games/{gameId}/notes/{noteIndex}")
    public ResponseEntity<MessageResponse> removeMyReaction(
            @PathVariable Long gameId,
            @PathVariable Integer noteIndex
    ) {
        noteReactionService.removeReaction(currentUserService.requireUsername(), gameId, noteIndex);
        return ResponseEntity.ok(new MessageResponse("Reaction removed"));
    }

    @GetMapping("/games/{gameId}/notes/{noteIndex}")
    public ResponseEntity<NoteReactionSummaryDTO> getReactionSummary(
            @PathVariable Long gameId,
            @PathVariable Integer noteIndex
    ) {
        String username = currentUserService.getUsername().orElse(null);
        return ResponseEntity.ok(noteReactionService.getReactionSummary(gameId, noteIndex, username));
    }

    @GetMapping("/games/{gameId}/notes/{noteIndex}/all")
    public ResponseEntity<List<NoteReactionResponseDTO>> getAllReactionsForNote(
            @PathVariable Long gameId,
            @PathVariable Integer noteIndex
    ) {
        return ResponseEntity.ok(noteReactionService.getReactionsForNote(gameId, noteIndex));
    }
}
