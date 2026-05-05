package com.proyectoflutter.backend_api.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.GameNote;
import com.proyectoflutter.backend_api.models.NoteReaction;
import com.proyectoflutter.backend_api.models.Reaction;
import com.proyectoflutter.backend_api.models.User;
import com.proyectoflutter.backend_api.payload.response.NoteReactionResponseDTO;
import com.proyectoflutter.backend_api.payload.response.NoteReactionSummaryDTO;
import com.proyectoflutter.backend_api.repository.GameRepository;
import com.proyectoflutter.backend_api.repository.NoteReactionRepository;
import com.proyectoflutter.backend_api.repository.UserRepository;
import com.proyectoflutter.backend_api.services.reactions.ReactionSummaryStrategy;

@Service
public class NoteReactionService {

    // Orquestador de casos de uso: aplica validación, reglas de negocio y
    // armado de DTOs. Mantiene el controller libre de lógica y centraliza la
    // transacción para crear, resumir y borrar reacciones de una opinión.
    private final NoteReactionRepository noteReactionRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ReactionService reactionService;
    private final ReactionSummaryStrategy reactionSummaryStrategy;
    private final GameNoteService gameNoteService;

    public NoteReactionService(
            NoteReactionRepository noteReactionRepository,
            GameRepository gameRepository,
            UserRepository userRepository,
            ReactionService reactionService,
            ReactionSummaryStrategy reactionSummaryStrategy,
            GameNoteService gameNoteService
    ) {
        this.noteReactionRepository = noteReactionRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.reactionService = reactionService;
        this.reactionSummaryStrategy = reactionSummaryStrategy;
        this.gameNoteService = gameNoteService;
    }

    @Transactional
    public NoteReactionResponseDTO createOrUpdateReaction(
            String username,
            Long gameId,
            Integer noteIndex,
            Long reactionId
    ) {
        Game game = getGameAndValidateNoteIndex(gameId, noteIndex);
        int safeNoteIndex = noteIndex;
        gameNoteService.requireNotDeleted(game.getNotes().get(safeNoteIndex));
        User user = getUserByUsername(username);
        Reaction reaction = getReactionById(reactionId);

        // Prefer to link by persistent note id when available
        Long noteId = null;
        if (game.getNotes() != null && safeNoteIndex >= 0 && safeNoteIndex < game.getNotes().size()) {
            noteId = game.getNotes().get(safeNoteIndex).getId();
        }

        NoteReaction noteReaction;
        if (noteId != null) {
            noteReaction = noteReactionRepository
                    .findByUserIdAndGameIdAndNoteId(user.getId(), gameId, noteId)
                    .orElseGet(NoteReaction::new);
                noteReaction.setNoteIndex(safeNoteIndex);
            // set direct relation when possible
                GameNote note = game.getNotes().get(safeNoteIndex);
            noteReaction.setNote(note);
        } else {
            noteReaction = noteReactionRepository
                    .findByUserIdAndGameIdAndNoteIndex(user.getId(), gameId, noteIndex)
                    .orElseGet(NoteReaction::new);
                noteReaction.setNoteIndex(safeNoteIndex);
        }

        noteReaction.setUser(user);
        noteReaction.setGame(game);
        noteReaction.setReaction(reaction);

        NoteReaction saved = noteReactionRepository.save(noteReaction);
        return NoteReactionResponseDTO.fromEntity(saved);
    }

    @Transactional
    public void removeReaction(String username, Long gameId, Integer noteIndex) {
        Game game = getGameAndValidateNoteIndex(gameId, noteIndex);
        User user = getUserByUsername(username);
        // prefer deleting by note id when present
        Long noteId = null;
        if (game.getNotes() != null && noteIndex != null && noteIndex >= 0 && noteIndex < game.getNotes().size()) {
            noteId = game.getNotes().get(noteIndex).getId();
        }
        if (noteId != null) {
            noteReactionRepository.deleteByUserIdAndGameIdAndNoteId(user.getId(), game.getId(), noteId);
        } else {
            noteReactionRepository.deleteByUserIdAndGameIdAndNoteIndex(user.getId(), game.getId(), noteIndex);
        }
    }

    @Transactional(readOnly = true)
    public List<NoteReactionResponseDTO> getReactionsForNote(Long gameId, Integer noteIndex) {
        getGameAndValidateNoteIndex(gameId, noteIndex);
        Long noteId = null;
        // try to resolve note id
        Game g = gameRepository.findById(gameId).orElseThrow();
        if (g.getNotes() != null && noteIndex != null && noteIndex >= 0 && noteIndex < g.getNotes().size()) {
            noteId = g.getNotes().get(noteIndex).getId();
        }

        if (noteId != null) {
            return noteReactionRepository.findByGameIdAndNoteId(gameId, noteId)
                    .stream().map(NoteReactionResponseDTO::fromEntity).collect(Collectors.toList());
        }

        return noteReactionRepository.findByGameIdAndNoteIndex(gameId, noteIndex)
                .stream().map(NoteReactionResponseDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoteReactionSummaryDTO getReactionSummary(Long gameId, Integer noteIndex, String username) {
        getGameAndValidateNoteIndex(gameId, noteIndex);
        Long noteId = null;
        Game g = gameRepository.findById(gameId).orElseThrow();
        if (g.getNotes() != null && noteIndex != null && noteIndex >= 0 && noteIndex < g.getNotes().size()) {
            noteId = g.getNotes().get(noteIndex).getId();
        }

        List<NoteReaction> reactions = noteId != null
                ? noteReactionRepository.findByGameIdAndNoteId(gameId, noteId)
                : noteReactionRepository.findByGameIdAndNoteIndex(gameId, noteIndex);
        Map<String, Long> counts = reactionSummaryStrategy.countReactions(reactions);

        NoteReactionSummaryDTO summary = new NoteReactionSummaryDTO();
        summary.setGameId(gameId);
        summary.setNoteIndex(noteIndex);
        summary.setCounts(counts);
        summary.setTotal(reactions.size());

        if (username != null && !username.isBlank()) {
            Optional<NoteReaction> mine;
            if (noteId != null) {
                mine = noteReactionRepository.findByUserIdAndGameIdAndNoteId(getUserByUsername(username).getId(), gameId, noteId);
            } else {
                mine = noteReactionRepository.findByUserIdAndGameIdAndNoteIndex(getUserByUsername(username).getId(), gameId, noteIndex);
            }
            mine.map(NoteReactionResponseDTO::fromEntity).ifPresent(summary::setMyReaction);
        }

        return summary;
    }

    @Transactional
    public void handleNoteDeleted(Long gameId, Integer deletedNoteIndex) {
        noteReactionRepository.deleteByGameIdAndNoteIndex(gameId, deletedNoteIndex);
        noteReactionRepository.decrementNoteIndexesAfterDelete(gameId, deletedNoteIndex);
    }

    @Transactional
    public void deleteByGame(Long gameId) {
        noteReactionRepository.deleteByGameId(gameId);
    }

    private Game getGameAndValidateNoteIndex(Long gameId, Integer noteIndex) {
        if (noteIndex == null || noteIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note index must be >= 0");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with id: " + gameId));

        int notesSize = game.getNotes() == null ? 0 : game.getNotes().size();
        if (noteIndex >= notesSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note index out of bounds: " + noteIndex);
        }

        return game;
    }

    private User getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Reaction getReactionById(Long reactionId) {
        return reactionService.getReactionById(reactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reaction not found with id: " + reactionId));
    }
}
