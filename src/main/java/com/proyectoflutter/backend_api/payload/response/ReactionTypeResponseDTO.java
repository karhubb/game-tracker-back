package com.proyectoflutter.backend_api.payload.response;

import com.proyectoflutter.backend_api.models.Reaction;

public class ReactionTypeResponseDTO {

    private Long id;
    private String description;

    public ReactionTypeResponseDTO(Reaction reaction) {
        this.id = reaction.getId();
        this.description = reaction.getDescription().name();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
