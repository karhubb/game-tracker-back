package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerPutNotesTest {

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
    void putUpdatesGameAndLeavesNotesUntouched() throws Exception {
        Game existing = new Game();
        existing.setId(3L);
        existing.setName("Genshin Impact");
        existing.setCoverUrl("https://example.com/cover.jpg");
        existing.setFranchise("Hoyoverse");
        existing.setCategory("Mundo Abierto");
        existing.setRating(1);
        existing.setPlayed(true);

        when(gameRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserService.hasRole("ROLE_ADMIN")).thenReturn(true);

        String payload = """
                {
                  "name": "Genshin Impact",
                  "coverUrl": "https://example.com/cover.jpg",
                  "franchise": "Hoyoverse",
                  "category": "Mundo Abierto",
                  "rating": 1,
                  "played": true,
                  "notes": [
                    {
                      "content": "Bodrio",
                      "date": "2026-04-09T17:45:19"
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/juegos/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());

        Game saved = captor.getValue();
        assertNotNull(saved.getNotesJson());
        assertEquals(0, saved.getNotes().size());
    }
}
