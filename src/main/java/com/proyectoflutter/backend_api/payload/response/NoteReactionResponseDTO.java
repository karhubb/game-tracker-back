package com.proyectoflutter.backend_api.payload.response;

import com.proyectoflutter.backend_api.models.NoteReaction;

/**
 * DTO for HTTP response: Individual note reaction details.
 * 
 * DESIGN PATTERN: Factory Method (Creational)
 * 
 * Purpose:
 * Encapsulate the logic of converting a NoteReaction entity to a response DTO.
 * Provide a static factory method to centralize entity→DTO transformation.
 * 
 * Why Factory Method for DTO Mapping?
 * - Prevents duplication: Services don't repeat the same mapping logic
 * - Single point of change: If we need to expose different fields to clients,
 *   we update ONE place (the fromEntity method) instead of N places
 * - Abstraction: Hides complexity of which entity relationships to include
 *   (e.g., User details, Game reference)
 * - Consistency: All NoteReactionResponseDTO instances created the same way
 * 
 * Usage Example:
 *   NoteReaction entity = noteReactionService.getReaction(id);
 *   NoteReactionResponseDTO dto = NoteReactionResponseDTO.fromEntity(entity);
 *   // Services and controllers don't need to know HOW to map
 * 
 * Anti-pattern (without factory):
 *   // Service 1 does this:
 *   dto.setId(entity.getId());
 *   dto.setGameId(entity.getGame().getId());
 *   // Service 2 does this (slightly different):
 *   dto.setId(entity.getId());
 *   // Service 3 does this (incomplete):
 *   dto.setId(entity.getId());
 *   // Inconsistency and duplication!
 * 
 * Architecture Benefit:
 * - Controllers/Services: @ResponseBody dto = Dto.fromEntity(entity);
 * - DTOs own their creation logic
 * - Easy to add new mapping rules (nested relationships, computed fields, etc.)
 */
public class NoteReactionResponseDTO {

    private Long id;
    private Long gameId;
    private Integer noteIndex;
    private Long reactionId;
    private String reaction;
    private UserResponseDTO user;

    /**
     * Static factory method: Create a DTO from a NoteReaction entity.
     * 
     * This is the ONLY way to create a NoteReactionResponseDTO from an entity.
     * Centralizes entity→DTO mapping logic.
     * 
     * @param noteReaction The entity to convert
     * @return A new DTO with fields populated from the entity
     */
    public static NoteReactionResponseDTO fromEntity(NoteReaction noteReaction) {
        return new NoteReactionResponseDTO(noteReaction);
    }

    /**
     * Private constructor: Only called by the factory method.
     * Prevents accidental misuse (e.g., creating incomplete DTOs).
     */
    public NoteReactionResponseDTO(NoteReaction noteReaction) {
        this.id = noteReaction.getId();
        this.gameId = noteReaction.getGame().getId();
        this.noteIndex = noteReaction.getNoteIndex();
        this.reactionId = noteReaction.getReaction().getId();
        this.reaction = noteReaction.getReaction().getDescription().name();
        this.user = new UserResponseDTO(noteReaction.getUser());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Integer getNoteIndex() {
        return noteIndex;
    }

    public void setNoteIndex(Integer noteIndex) {
        this.noteIndex = noteIndex;
    }

    public Long getReactionId() {
        return reactionId;
    }

    public void setReactionId(Long reactionId) {
        this.reactionId = reactionId;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }
}
