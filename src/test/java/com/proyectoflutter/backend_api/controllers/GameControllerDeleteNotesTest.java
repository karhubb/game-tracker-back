package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.GameNote;
import com.proyectoflutter.backend_api.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerDeleteNotesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameRepository gameRepository;
    @MockitoBean
    private com.proyectoflutter.backend_api.services.NoteReactionService noteReactionService;
    @MockitoBean
    private com.proyectoflutter.backend_api.security.services.CurrentUserService currentUserService;
    @MockitoBean
    private com.proyectoflutter.backend_api.services.NoteAuthorizationService noteAuthorizationService;
    @MockitoBean
    private com.proyectoflutter.backend_api.services.GameNoteService gameNoteService;

    @Test
    void deleteLeafNoteRemovesOnlyThatNote() throws Exception {
        Game existing = new Game();
        existing.setId(7L);
        existing.setName("Game");
        existing.setCoverUrl("cover");
        existing.setFranchise("franchise");
        existing.setCategory("category");
        existing.setRating(5);
        existing.setPlayed(true);

        GameNote first = new GameNote();
        first.setContent("First");
        first.setDate(LocalDateTime.now());
        first.setAuthorUsername("author1");

        GameNote second = new GameNote();
        second.setContent("Second");
        second.setDate(LocalDateTime.now());
        second.setAuthorUsername("author2");

        existing.setNotes(List.of(first, second));

        when(gameRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/api/juegos/7/notes/0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(noteReactionService).handleNoteDeleted(7L, 0);
        verify(noteReactionService, never()).handleNoteDeleted(7L, 1);

        assertEquals(1, existing.getNotes().size());
        assertEquals("Second", existing.getNotes().get(0).getContent());
        assertFalse(existing.getNotes().get(0).isDeleted());
    }

    @Test
    void deleteNoteWithChildrenMarksItDeletedWithoutRemovingReplies() throws Exception {
        Game existing = new Game();
        existing.setId(8L);
        existing.setName("Game");
        existing.setCoverUrl("cover");
        existing.setFranchise("franchise");
        existing.setCategory("category");
        existing.setRating(5);
        existing.setPlayed(true);

        GameNote root = new GameNote();
        root.setContent("Root");
        root.setDate(LocalDateTime.now());
        root.setAuthorUsername("author1");

        GameNote reply = new GameNote();
        reply.setContent("Reply");
        reply.setDate(LocalDateTime.now());
        reply.setAuthorUsername("author2");
        reply.setParent(root);

        existing.setNotes(List.of(root, reply));

        when(gameRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/api/juegos/8/notes/0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(noteReactionService, never()).handleNoteDeleted(any(), any());

        assertEquals(2, existing.getNotes().size());
        assertTrue(existing.getNotes().get(0).isDeleted());
        assertEquals("El contenido de este comentario se ha eliminado.", existing.getNotes().get(0).getContent());
        assertEquals("Reply", existing.getNotes().get(1).getContent());
        assertFalse(existing.getNotes().get(1).isDeleted());
    }
}
