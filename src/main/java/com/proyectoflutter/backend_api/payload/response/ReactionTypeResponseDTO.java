package com.proyectoflutter.backend_api.payload.response;

import com.proyectoflutter.backend_api.models.Reaction;

/**
 * DTO for HTTP response: Reaction type catalog.
 * 
 * DESIGN PATTERN: Factory Method (Creational)
 * 
 * Purpose:
 * Encapsulate the logic of converting a Reaction entity to a response DTO.
 * Provide a static factory method to centralize entity→DTO transformation.
 * 
 * Why Factory Method for DTO Mapping?
 * - Single Responsibility: Controller routes HTTP; DTO owns creation logic
 * - Prevents Duplication: If multiple endpoints return ReactionTypes,
 *   they all use the same transformation (DRY principle)
 * - Easy to Extend: To add computed fields (e.g., userCanReact, reactionCount),
 *   update ONE method instead of multiple locations
 * - Consistency: All ReactionTypeResponseDTO instances are created identically
 * 
 * Usage Example:
 *   List<Reaction> reactions = reactionService.getAllReactions();
 *   List<ReactionTypeResponseDTO> dtos = reactions.stream()
 *       .map(ReactionTypeResponseDTO::fromEntity)
 *       .collect(Collectors.toList());
 *   // All DTOs created the same way; controller is thin
 * 
 * Data Flow:
 *   Reaction entity {id:1, description:FUNNY}
 *     ↓ (fromEntity)
 *   ReactionTypeResponseDTO {id:1, description:"FUNNY"}
 *     ↓ (JSON serialization)
 *   HTTP Response: {"id":1,"description":"FUNNY"}
 * 
 * Benefits:
 * - Decouples controllers from entity structure
 * - DTOs can expose different views (e.g., admin view vs. public view)
 * - Easy to add validation or transformation rules
 */
public class ReactionTypeResponseDTO {

    private Long id;
    private String description;

    /**
     * Static factory method: Create a DTO from a Reaction entity.
     * 
     * This is the canonical way to convert Reaction entities to DTOs.
     * Centralizes all entity→DTO mapping logic.
     * 
     * @param reaction The entity to convert
     * @return A new DTO with id and description populated
     */
    public static ReactionTypeResponseDTO fromEntity(Reaction reaction) {
        return new ReactionTypeResponseDTO(reaction);
    }

    /**
     * Private constructor: Enforces use of factory method.
     * Prevents incomplete DTO creation (e.g., forgetting to set description).
     */
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
