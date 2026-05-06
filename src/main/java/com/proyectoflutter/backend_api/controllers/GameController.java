package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.DeleteStrategy;
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
    // Supports multiple deletion strategies:
    // - SOFT_DELETE (default): Mark deleted with placeholder, preserve structure
    // - HARD_DELETE: Remove note only (if no children, throws error if has children)
    // - CASCADE_DELETE: Remove note + all descendants (admin-only)
    @DeleteMapping("/{id}/notes/{noteIndex}")
    @Transactional
    public Game deleteGameNote(
            @PathVariable Long id,
            @PathVariable int noteIndex,
            @RequestParam(value = "strategy", defaultValue = "SOFT_DELETE") String strategyParam
    ) {
        Game game = getGameOrThrow(id);
        List<GameNote> notes = game.getNotes();
        validateNoteIndex(notes, noteIndex);

        DeleteStrategy strategy;
        try {
            strategy = DeleteStrategy.valueOf(strategyParam);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid delete strategy: " + strategyParam + ". Valid values: SOFT_DELETE, HARD_DELETE, CASCADE_DELETE"
            );
        }

        GameNote target = notes.get(noteIndex);
        gameNoteService.requireNotDeleted(target);
        requireNoteDeletePermission(target);
        noteAuthorizationService.requireDeleteStrategy(target, strategy);

        switch (strategy) {
            case SOFT_DELETE:
                performSoftDelete(target);
                break;
            case HARD_DELETE:
                if (!target.getChildren().isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Cannot hard-delete note with children. Use CASCADE_DELETE (admin-only) to remove all replies."
                    );
                }
                performHardDelete(notes, noteIndex);
                break;
            case CASCADE_DELETE:
                performCascadeDelete(game, notes, noteIndex);
                break;
        }

        return gameRepository.save(game);
    }

    /**
     * Soft delete: Mark note as deleted with placeholder text, preserve tree structure.
     * Reactions are NOT deleted (to preserve like counts in database).
     */
    private void performSoftDelete(GameNote target) {
        target.setContent(GameNoteService.DELETED_PLACEHOLDER);
        target.setDeleted(true);
    }

    /**
     * Hard delete: Physically remove note from list. Only allowed if no children.
     * Reindex remaining notes.
     */
    private void performHardDelete(List<GameNote> notes, int noteIndex) {
        GameNote target = notes.get(noteIndex);
        GameNote parent = target.getParent();
        if (parent != null) {
            parent.getChildren().remove(target);
        }

        noteReactionService.handleNoteDeleted(notes.get(0).getGame().getId(), noteIndex);
        notes.remove(noteIndex);
        reindexParentIndexes(notes);
    }

    /**
     * Cascade delete: Remove note and all descendants recursively.
     * Admin-only operation. Cleans up all reactions for deleted notes.
     */
    private void performCascadeDelete(Game game, List<GameNote> notes, int targetIndex) {
        GameNote target = notes.get(targetIndex);
        Long gameId = game.getId();

        // Collect all indices to delete: target + all descendants (in reverse order)
        List<Integer> indicesToDelete = new java.util.ArrayList<>();
        indicesToDelete.add(targetIndex);
        
        // Find and add all descendants
        for (int i = targetIndex + 1; i < notes.size(); i++) {
            if (isDescendant(notes, i, targetIndex)) {
                indicesToDelete.add(i);
            }
        }

        // Delete in reverse order to avoid index shifting issues
        for (int i = indicesToDelete.size() - 1; i >= 0; i--) {
            int idx = indicesToDelete.get(i);
            GameNote noteToDelete = notes.get(idx);
            
            // Remove from parent's children list
            GameNote parent = noteToDelete.getParent();
            if (parent != null) {
                parent.getChildren().remove(noteToDelete);
            }

            // Clean up reactions
            noteReactionService.handleNoteDeleted(gameId, idx);
        }

        // Remove all notes (in reverse order to preserve indices during removal)
        for (int i = indicesToDelete.size() - 1; i >= 0; i--) {
            notes.remove((int) indicesToDelete.get(i));
        }

        // Reindex remaining notes
        reindexParentIndexes(notes);
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