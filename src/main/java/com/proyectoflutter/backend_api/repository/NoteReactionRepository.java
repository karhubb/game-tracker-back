package com.proyectoflutter.backend_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.proyectoflutter.backend_api.models.NoteReaction;

@Repository
public interface NoteReactionRepository extends JpaRepository<NoteReaction, Long> {

    List<NoteReaction> findByGameIdAndNoteIndex(Long gameId, Integer noteIndex);

    Optional<NoteReaction> findByUserIdAndGameIdAndNoteIndex(Long userId, Long gameId, Integer noteIndex);

    @Modifying
    @Query("delete from NoteReaction nr where nr.game.id = :gameId and nr.noteIndex = :noteIndex")
    void deleteByGameIdAndNoteIndex(@Param("gameId") Long gameId, @Param("noteIndex") Integer noteIndex);

    @Modifying
    @Query("delete from NoteReaction nr where nr.game.id = :gameId")
    void deleteByGameId(@Param("gameId") Long gameId);

    @Modifying
    @Query("update NoteReaction nr set nr.noteIndex = nr.noteIndex - 1 where nr.game.id = :gameId and nr.noteIndex > :deletedIndex")
    int decrementNoteIndexesAfterDelete(@Param("gameId") Long gameId, @Param("deletedIndex") Integer deletedIndex);

    long countByGameIdAndNoteIndexAndReactionId(Long gameId, Integer noteIndex, Long reactionId);

    void deleteByUserIdAndGameIdAndNoteIndex(Long userId, Long gameId, Integer noteIndex);
}
