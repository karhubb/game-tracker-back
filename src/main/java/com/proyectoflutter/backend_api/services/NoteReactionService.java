package com.proyectoflutter.backend_api.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoflutter.backend_api.models.EReaction;
import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.NoteReaction;
import com.proyectoflutter.backend_api.models.Reaction;
import com.proyectoflutter.backend_api.models.User;
import com.proyectoflutter.backend_api.payload.response.NoteReactionResponseDTO;
import com.proyectoflutter.backend_api.payload.response.NoteReactionSummaryDTO;
import com.proyectoflutter.backend_api.repository.GameRepository;
import com.proyectoflutter.backend_api.repository.NoteReactionRepository;
import com.proyectoflutter.backend_api.repository.ReactionRepository;
import com.proyectoflutter.backend_api.repository.UserRepository;

@Service
public class NoteReactionService {

    private final NoteReactionRepository noteReactionRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;

    public NoteReactionService(
            NoteReactionRepository noteReactionRepository,
            GameRepository gameRepository,
            UserRepository userRepository,
            ReactionRepository reactionRepository
    ) {
        this.noteReactionRepository = noteReactionRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.reactionRepository = reactionRepository;
    }

    @Transactional
    public NoteReactionResponseDTO createOrUpdateReaction(
            String username,
            Long gameId,
            Integer noteIndex,
            Long reactionId
    ) {
        Game game = getGameAndValidateNoteIndex(gameId, noteIndex);
        User user = getUserByUsername(username);
        Reaction reaction = getReactionById(reactionId);

        NoteReaction noteReaction = noteReactionRepository
                .findByUserIdAndGameIdAndNoteIndex(user.getId(), gameId, noteIndex)
                .orElseGet(NoteReaction::new);

        noteReaction.setUser(user);
        noteReaction.setGame(game);
        noteReaction.setNoteIndex(noteIndex);
        noteReaction.setReaction(reaction);

        NoteReaction saved = noteReactionRepository.save(noteReaction);
        return new NoteReactionResponseDTO(saved);
    }

    @Transactional
    public void removeReaction(String username, Long gameId, Integer noteIndex) {
        Game game = getGameAndValidateNoteIndex(gameId, noteIndex);
        User user = getUserByUsername(username);
        noteReactionRepository.deleteByUserIdAndGameIdAndNoteIndex(user.getId(), game.getId(), noteIndex);
    }

    @Transactional(readOnly = true)
    public List<NoteReactionResponseDTO> getReactionsForNote(Long gameId, Integer noteIndex) {
        getGameAndValidateNoteIndex(gameId, noteIndex);
        return noteReactionRepository.findByGameIdAndNoteIndex(gameId, noteIndex)
                .stream()
                .map(NoteReactionResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoteReactionSummaryDTO getReactionSummary(Long gameId, Integer noteIndex, String username) {
        getGameAndValidateNoteIndex(gameId, noteIndex);

        List<NoteReaction> reactions = noteReactionRepository.findByGameIdAndNoteIndex(gameId, noteIndex);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (EReaction reactionType : EReaction.values()) {
            long total = reactions.stream()
                    .filter(r -> r.getReaction().getDescription() == reactionType)
                    .count();
            counts.put(reactionType.name(), total);
        }

        NoteReactionSummaryDTO summary = new NoteReactionSummaryDTO();
        summary.setGameId(gameId);
        summary.setNoteIndex(noteIndex);
        summary.setCounts(counts);
        summary.setTotal(reactions.size());

        if (username != null && !username.isBlank()) {
            Optional<NoteReaction> mine = noteReactionRepository
                    .findByUserIdAndGameIdAndNoteIndex(getUserByUsername(username).getId(), gameId, noteIndex);
            mine.map(NoteReactionResponseDTO::new).ifPresent(summary::setMyReaction);
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
        return reactionRepository.findById(reactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reaction not found with id: " + reactionId));
    }
}
