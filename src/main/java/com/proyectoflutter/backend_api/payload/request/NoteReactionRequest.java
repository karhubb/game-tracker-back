package com.proyectoflutter.backend_api.payload.request;

import jakarta.validation.constraints.NotNull;

public class NoteReactionRequest {

    @NotNull
    private Long reactionId;

    public Long getReactionId() {
        return reactionId;
    }

    public void setReactionId(Long reactionId) {
        this.reactionId = reactionId;
    }
}
