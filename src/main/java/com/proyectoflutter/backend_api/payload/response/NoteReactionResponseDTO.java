package com.proyectoflutter.backend_api.payload.response;

import com.proyectoflutter.backend_api.models.NoteReaction;

public class NoteReactionResponseDTO {

    private Long id;
    private Long gameId;
    private Integer noteIndex;
    private Long reactionId;
    private String reaction;
    private UserResponseDTO user;

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
