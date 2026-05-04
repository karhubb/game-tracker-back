package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.GameNote;
import com.proyectoflutter.backend_api.repository.GameRepository;
import com.proyectoflutter.backend_api.services.NoteReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/juegos")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private NoteReactionService noteReactionService;

    private Game getGameOrThrow(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with id: " + id));
    }

    private void validateNoteIndex(List<GameNote> notes, int noteIndex) {
        if (noteIndex < 0 || noteIndex >= notes.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Note index out of bounds: " + noteIndex
            );
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return authentication.getName();
    }

    private boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> roleName.equals(auth.getAuthority()));
    }

    private void requireAdmin() {
        if (!hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can modify games");
        }
    }

    private void requireNoteEditPermission(GameNote note) {
        String username = currentUsername();
        boolean isAdmin = hasRole("ROLE_ADMIN");

        if (isAdmin) {
            return;
        }

        if (note.getAuthorUsername() == null || !note.getAuthorUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can edit only your own notes");
        }
    }

    private void requireNoteDeletePermission(GameNote note) {
        String username = currentUsername();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        boolean isModerator = hasRole("ROLE_MODERATOR");

        if (isAdmin || isModerator) {
            return;
        }

        if (note.getAuthorUsername() == null || !note.getAuthorUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can delete only your own notes");
        }
    }

    // LEER todos (GET)
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // CREAR uno nuevo (POST)
    @PostMapping
    @Transactional
    public Game createGame(@RequestBody Game game) {
        requireAdmin();
        return gameRepository.save(game);
    }

    // EDITAR un juego existente (PUT)
    @PutMapping("/{id}")
    @Transactional
    public Game updateGame(@PathVariable Long id, @RequestBody Game gameDetails) {
        requireAdmin();
        Game game = getGameOrThrow(id);

        game.setName(gameDetails.getName());
        game.setCoverUrl(gameDetails.getCoverUrl());
        game.setFranchise(gameDetails.getFranchise());
        game.setCategory(gameDetails.getCategory());
        game.setRating(gameDetails.getRating());
        game.setPlayed(gameDetails.getPlayed());
        game.setNotes(gameDetails.getNotes());
        // serializeNotes() es llamado automáticamente por setNotes()

        return gameRepository.save(game);
    }

    // CREAR una opinión específica (POST)
    @PostMapping("/{id}/notes")
    @Transactional
    public Game addGameNote(@PathVariable Long id, @RequestBody GameNote noteDetails) {
        Game game = getGameOrThrow(id);

        if (noteDetails.getContent() == null || noteDetails.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
        }

        List<GameNote> notes = game.getNotes();
        GameNote newNote = new GameNote();
        newNote.setContent(noteDetails.getContent().trim());
        newNote.setDate(noteDetails.getDate() != null ? noteDetails.getDate() : java.time.LocalDateTime.now());
        newNote.setAuthorUsername(currentUsername());

        notes.add(newNote);
        game.setNotes(notes);

        return gameRepository.save(game);
    }

    // EDITAR una opinión específica (PUT)
    @PutMapping("/{id}/notes/{noteIndex}")
    @Transactional
    public Game updateGameNote(
            @PathVariable Long id,
            @PathVariable int noteIndex,
            @RequestBody GameNote noteDetails
    ) {
        Game game = getGameOrThrow(id);
        List<GameNote> notes = game.getNotes();
        validateNoteIndex(notes, noteIndex);

        GameNote current = notes.get(noteIndex);
        requireNoteEditPermission(current);
        if (noteDetails.getContent() == null || noteDetails.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
        }

        current.setContent(noteDetails.getContent().trim());
        if (noteDetails.getDate() != null) {
            current.setDate(noteDetails.getDate());
        }
        if (current.getAuthorUsername() == null || current.getAuthorUsername().isBlank()) {
            current.setAuthorUsername(currentUsername());
        }

        notes.set(noteIndex, current);
        game.setNotes(notes);

        return gameRepository.save(game);
    }

    // ELIMINAR una opinión específica (DELETE)
    @DeleteMapping("/{id}/notes/{noteIndex}")
    @Transactional
    public Game deleteGameNote(@PathVariable Long id, @PathVariable int noteIndex) {
        Game game = getGameOrThrow(id);
        List<GameNote> notes = game.getNotes();
        validateNoteIndex(notes, noteIndex);

        requireNoteDeletePermission(notes.get(noteIndex));

        noteReactionService.handleNoteDeleted(id, noteIndex);
        notes.remove(noteIndex);
        game.setNotes(notes);

        return gameRepository.save(game);
    }

    // ELIMINAR (DELETE)
    @DeleteMapping("/{id}")
    @Transactional
    public void deleteGame(@PathVariable Long id) {
        requireAdmin();
        noteReactionService.deleteByGame(id);
        gameRepository.deleteById(id);
    }
}