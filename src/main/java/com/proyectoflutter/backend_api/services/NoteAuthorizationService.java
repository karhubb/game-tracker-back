package com.proyectoflutter.backend_api.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoflutter.backend_api.models.GameNote;
import com.proyectoflutter.backend_api.security.services.CurrentUserService;

/**
 * DESIGN PATTERN: Domain-level Authorization / Policy Object (Behavioral)
 * 
 * Purpose:
 * Encapsulate all permission rules for GameNote access in one place.
 * Controllers delegate authorization decisions without mixing access control with HTTP routing.
 * 
 * Why This Pattern?
 * - Note permission logic was duplicated in GameController.requireNoteEditPermission()
 *   and GameController.requireNoteDeletePermission()
 * - Permission requirements evolve: "moderators can delete notes", "only admins can unpin", etc.
 * - Business rules should be testable and independent from controllers
 * - Single Responsibility: GameController routes HTTP; NoteAuthorizationService enforces rules
 * - Centralization: Change permission logic in ONE place, not scattered across controllers
 * 
 * Permission Model:
 * - EDIT: Current user (author) OR admin (full access)
 * - DELETE: Current user (author) OR admin OR moderator (elevated rights)
 * 
 * Usage Example:
 *   GameNote note = gameRepository.findNote(gameId, noteIndex);
 *   noteAuthorizationService.requireNoteEditPermission(note); // Throws if unauthorized
 *   // ... now safe to update note ...
 * 
 * Architecture Benefits:
 * - Controllers become thin: They route requests and call services
 * - Services own business logic: "Who can edit?" is a business rule, not HTTP logic
 * - Easy to test: Inject mock CurrentUserService, verify permission checks
 * - Easy to extend: Add new permission methods (e.g., requireNoteSharePermission)
 * - Audit trail: All permission denials go through one point (easier to log/monitor)
 */
@Service
public class NoteAuthorizationService {

    private final CurrentUserService currentUserService;

    public NoteAuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /**
     * Verify that the current user can edit the given note.
     * 
     * Permission: User is the note's author OR user is admin.
     * 
     * @param note The GameNote to check permission for
     * @throws ResponseStatusException with HttpStatus.FORBIDDEN if user lacks permission
     */
    public void requireNoteEditPermission(GameNote note) {
        String username = currentUserService.requireUsername();
        boolean isAdmin = currentUserService.hasRole("ROLE_ADMIN");

        // Admin bypass: full access to all notes
        if (isAdmin) {
            return;
        }

        // Author check: user can edit only their own notes
        if (note.getAuthorUsername() == null || !note.getAuthorUsername().equals(username)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can edit only your own notes"
            );
        }
    }

    /**
     * Verify that the current user can delete the given note.
     * 
     * Permission: User is the note's author OR user is admin OR user is moderator.
     * Note: Moderators have elevated rights to remove notes without owning them.
     * 
     * @param note The GameNote to check permission for
     * @throws ResponseStatusException with HttpStatus.FORBIDDEN if user lacks permission
     */
    public void requireNoteDeletePermission(GameNote note) {
        String username = currentUserService.requireUsername();
        boolean isAdmin = currentUserService.hasRole("ROLE_ADMIN");
        boolean isModerator = currentUserService.hasRole("ROLE_MODERATOR");

        // Admin/Moderator bypass: elevated rights to delete any note
        if (isAdmin || isModerator) {
            return;
        }

        // Author check: user can delete only their own notes
        if (note.getAuthorUsername() == null || !note.getAuthorUsername().equals(username)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can delete only your own notes"
            );
        }
    }
}
