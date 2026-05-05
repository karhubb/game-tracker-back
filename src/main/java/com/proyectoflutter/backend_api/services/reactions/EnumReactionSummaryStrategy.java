package com.proyectoflutter.backend_api.services.reactions;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.proyectoflutter.backend_api.models.EReaction;
import com.proyectoflutter.backend_api.models.NoteReaction;

@Component
public class EnumReactionSummaryStrategy implements ReactionSummaryStrategy {

    /**
     * DESIGN PATTERN: Strategy Pattern (Concrete Implementation)
     * 
     * Algorithm: O(n) single-pass counting using EnumMap for type-safe access.
     * 
     * Performance Benefits:
     * - Naive approach (filter per type): O(n*m) where m = number of reaction types
     * - This approach: O(n) with single pass through reactions list
     * - EnumMap: More efficient than HashMap for enum keys (uses array internally)
     * 
     * Why EnumMap + LinkedHashMap?
     * - EnumMap<EReaction, Long>: Type-safe, fast access during counting phase
     * - LinkedHashMap result: Preserves Java enum declaration order in response
     *   (FUNNY, INTERESTING, USELESS appear in predictable order to client)
     * 
     * Implementation Flow:
     * 1. Initialize EnumMap with all EReaction values (count = 0)
     * 2. Single pass: for each NoteReaction, increment its type counter
     * 3. Convert to LinkedHashMap preserving enum order
     * 4. Return Map<String, Long> where String = type name ("FUNNY", "INTERESTING")
     * 
     * Example:
     *   Input:  [NoteReaction(FUNNY), NoteReaction(FUNNY), NoteReaction(INTERESTING)]
     *   Output: {"FUNNY": 2, "INTERESTING": 1, "USELESS": 0}
     *   Order:  Matches EReaction enum declaration order
     */
    @Override
    public Map<String, Long> countReactions(List<NoteReaction> reactions) {
        EnumMap<EReaction, Long> counts = new EnumMap<>(EReaction.class);
        for (EReaction reactionType : EReaction.values()) {
            counts.put(reactionType, 0L);
        }

        for (NoteReaction noteReaction : reactions) {
            EReaction reactionType = noteReaction.getReaction().getDescription();
            counts.put(reactionType, counts.get(reactionType) + 1L);
        }

        Map<String, Long> orderedCounts = new LinkedHashMap<>();
        for (EReaction reactionType : EReaction.values()) {
            orderedCounts.put(reactionType.name(), counts.get(reactionType));
        }

        return orderedCounts;
    }
}