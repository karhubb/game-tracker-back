package com.proyectoflutter.backend_api.controllers;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class GameControllerPutNotesMariaDbTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @BeforeEach
    void setUp() {
        gameRepository.deleteAll();
    }

    @Test
    void putPersistsNotesJsonInMariaDb() throws Exception {
        Game game = new Game();
        game.setName("Genshin Impact");
        game.setCoverUrl("https://example.com/cover.jpg");
        game.setFranchise("Hoyoverse");
        game.setCategory("Mundo Abierto");
        game.setRating(1);
        game.setPlayed(true);
        Game saved = gameRepository.save(game);

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

        mockMvc.perform(put("/api/juegos/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Optional<Game> reloadedOpt = gameRepository.findById(saved.getId());
        Game reloaded = reloadedOpt.orElseThrow();

        assertTrue(reloaded.getNotesJson().contains("\"content\":\"Bodrio\""));
        assertEquals(1, reloaded.getNotes().size());
        assertEquals("Bodrio", reloaded.getNotes().get(0).getContent());
    }
}