package com.proyectoflutter.backend_api.services.reactions;

import java.util.List;
import java.util.Map;

import com.proyectoflutter.backend_api.models.NoteReaction;

/**
 * DESIGN PATTERN: Strategy Pattern (Behavioral)
 * 
 * Purpose:
 * Define a family of algorithms (reaction counting strategies) that can be 
 * swapped at runtime. This contract allows multiple implementations without 
 * forcing clients to know implementation details.
 * 
 * Why This Pattern?
 * - The naive approach iterates reactions per type: O(n*m) complexity
 * - Different strategies may optimize for memory vs. speed
 * - Strategies can be tested/benchmarked independently
 * - New strategies can be added without modifying existing code (Open/Closed Principle)
 * 
 * Example Usage:
 *   List<NoteReaction> reactions = noteReactionService.getReactionsForNote(gameId, noteIndex);
 *   Map<String, Long> summary = reactionSummaryStrategy.countReactions(reactions);
 *   // Result: {"FUNNY": 5, "INTERESTING": 2, "USELESS": 1}
 * 
 * Current Implementation: EnumReactionSummaryStrategy
 * - Uses EnumMap for O(n) single-pass counting
 * - Preserves Java enum order
 * - Returns LinkedHashMap for predictable iteration order
 */
public interface ReactionSummaryStrategy {

    /**
     * Count occurrences of each reaction type in a collection.
     * 
     * @param reactions List of NoteReaction entities to count
     * @return Map where key is reaction type name (e.g., "FUNNY") 
     *         and value is count of that reaction type
     */
    Map<String, Long> countReactions(List<NoteReaction> reactions);
}
