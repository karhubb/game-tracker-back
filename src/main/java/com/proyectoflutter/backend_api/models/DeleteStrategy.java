package com.proyectoflutter.backend_api.models;

/**
 * Enum representing different strategies for deleting a GameNote.
 * 
 * SOFT_DELETE: Mark note as deleted with placeholder text. Preserves thread structure.
 *   - Available to: Authors, Moderators, Admins
 *   - Behavior: Sets deleted=true, content="El contenido de este comentario se ha eliminado."
 *   - Children: Remain intact with parent reference preserved
 * 
 * HARD_DELETE: Physically remove note. Reindex remaining tree.
 *   - Available to: Authors (leaf only), Moderators, Admins
 *   - Behavior: Remove from notes list, delete reactions, reindex parentIndex
 *   - Constraints: Only allowed if note has no children (orphaned reply prevention)
 * 
 * CASCADE_DELETE: Remove note and all descendants recursively.
 *   - Available to: Admins only
 *   - Behavior: Delete target + all children + all reactions in cascade
 *   - Constraints: Only admins can use this (strict enforcement)
 */
public enum DeleteStrategy {
    SOFT_DELETE,
    HARD_DELETE,
    CASCADE_DELETE
}
