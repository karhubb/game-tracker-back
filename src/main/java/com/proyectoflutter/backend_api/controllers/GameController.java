package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.GameNote;
import com.proyectoflutter.backend_api.repository.GameRepository;
import com.proyectoflutter.backend_api.security.services.CurrentUserService;
import com.proyectoflutter.backend_api.services.GameNoteService;
import com.proyectoflutter.backend_api.services.NoteAuthorizationService;
import com.proyectoflutter.backend_api.services.NoteReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/juegos")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private NoteReactionService noteReactionService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private NoteAuthorizationService noteAuthorizationService;

    @Autowired
    private GameNoteService gameNoteService;

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

    private boolean hasRole(String roleName) {
        return currentUserService.hasRole(roleName);
    }

    private void requireAdmin() {
        if (!hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can modify games");
        }
    }

    private void requireNoteEditPermission(GameNote note) {
        noteAuthorizationService.requireNoteEditPermission(note);
    }

    private void requireNoteDeletePermission(GameNote note) {
        noteAuthorizationService.requireNoteDeletePermission(note);
    }

    /**
     * Recalcula y persiste parentIndex en todas las notas de la lista
     * según su referencia parent. Necesario después de cualquier inserción
     * o borrado que pueda haber desplazado los índices.
     */
    private void reindexParentIndexes(List<GameNote> notes) {
        Map<GameNote, Integer> indexByNote = new IdentityHashMap<>();
        Map<Long, Integer> indexById = new HashMap<>();

        for (int i = 0; i < notes.size(); i++) {
            GameNote note = notes.get(i);
            indexByNote.put(note, i);
            if (note.getId() != null) {
                indexById.put(note.getId(), i);
            }
        }

        for (int i = 0; i < notes.size(); i++) {
            GameNote note = notes.get(i);
            GameNote parent = note.getParent();
            if (parent == null) {
                note.setParentIndex(null);
            } else {
                Integer parentIndex = indexByNote.get(parent);
                if (parentIndex == null && parent.getId() != null) {
                    parentIndex = indexById.get(parent.getId());
                }
                note.setParentIndex(parentIndex);
            }
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
        // NOTA: las notas se gestionan exclusivamente via /notes endpoints.
        // NO llamar game.setNotes() aquí.

        return gameRepository.save(game);
    }

    // CREAR una opinión específica (POST)
    @PostMapping("/{id}/notes")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public void addGameNote(@PathVariable Long id, @RequestBody GameNote noteDetails) {
        Game game = getGameOrThrow(id);

        if (noteDetails.getContent() == null || noteDetails.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
        }

        // IMPORTANTE: notes ES la misma referencia que game.notes (colección Hibernate).
        // No llamar game.setNotes(notes) — haría clear() sobre esta misma lista y borraría todo.
        List<GameNote> notes = game.getNotes();

        GameNote newNote = new GameNote();
        newNote.setContent(noteDetails.getContent().trim());
        newNote.setDate(noteDetails.getDate() != null ? noteDetails.getDate() : java.time.LocalDateTime.now());
        newNote.setAuthorUsername(currentUserService.requireUsername());
        newNote.setGame(game);

        Integer parentIndex = noteDetails.getParentIndex();

        if (parentIndex == null) {
            newNote.setParentIndex(null);
            notes.add(newNote);
        } else {
            if (parentIndex < 0 || parentIndex >= notes.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent index out of bounds: " + parentIndex);
            }
            // Insertar justo después del último descendiente del padre
            int insertAt = parentIndex + 1;
            for (int i = parentIndex + 1; i < notes.size(); i++) {
                if (!isDescendant(notes, i, parentIndex)) break;
                insertAt = i + 1;
            }
            newNote.setParent(notes.get(parentIndex));
            newNote.setParentIndex(parentIndex);
            notes.get(parentIndex).getChildren().add(newNote);
            notes.add(insertAt, newNote);
        }

        // Recalcular parentIndex de todas las notas (la inserción pudo desplazar índices)
        reindexParentIndexes(notes);

        gameRepository.save(game);
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
        gameNoteService.requireNotDeleted(current);
        requireNoteEditPermission(current);
        if (noteDetails.getContent() == null || noteDetails.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
        }

        current.setContent(noteDetails.getContent().trim());
        if (noteDetails.getDate() != null) {
            current.setDate(noteDetails.getDate());
        }
        if (current.getAuthorUsername() == null || current.getAuthorUsername().isBlank()) {
            current.setAuthorUsername(currentUserService.requireUsername());
        }

        // No llamar game.setNotes(notes) — mismo bug de referencia compartida.
        return gameRepository.save(game);
    }

    // ELIMINAR una opinión específica (DELETE)
    @DeleteMapping("/{id}/notes/{noteIndex}")
    @Transactional
    public Game deleteGameNote(@PathVariable Long id, @PathVariable int noteIndex) {
        Game game = getGameOrThrow(id);
        List<GameNote> notes = game.getNotes();
        validateNoteIndex(notes, noteIndex);

        GameNote target = notes.get(noteIndex);
        gameNoteService.requireNotDeleted(target);
        requireNoteDeletePermission(target);

        if (target.getChildren().isEmpty()) {
            GameNote parent = target.getParent();
            if (parent != null) {
                parent.getChildren().remove(target);
            }

            noteReactionService.handleNoteDeleted(id, noteIndex);
            notes.remove(noteIndex);

            // Recalcular parentIndex tras el borrado físico (los índices se desplazaron)
            reindexParentIndexes(notes);
        } else {
            target.setContent(GameNoteService.DELETED_PLACEHOLDER);
            target.setDeleted(true);
        }

        // No llamar game.setNotes(notes) — mismo bug de referencia compartida.
        return gameRepository.save(game);
    }

    // Helper: ¿es candidateIndex descendiente de ancestorIndex?
    private boolean isDescendant(List<GameNote> notes, int candidateIndex, int ancestorIndex) {
        Integer parentIndex = notes.get(candidateIndex).getParentIndex();
        while (parentIndex != null) {
            if (parentIndex == ancestorIndex) return true;
            if (parentIndex < 0 || parentIndex >= notes.size()) return false;
            parentIndex = notes.get(parentIndex).getParentIndex();
        }
        return false;
    }

    // ELIMINAR juego (DELETE)
    @DeleteMapping("/{id}")
    @Transactional
    public void deleteGame(@PathVariable Long id) {
        requireAdmin();
        noteReactionService.deleteByGame(id);
        gameRepository.deleteById(id);
    }
}