package com.proyectoflutter.backend_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import com.proyectoflutter.backend_api.models.Game;
import com.proyectoflutter.backend_api.models.GameNote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.transaction.Transactional;

@SpringBootTest
public class GameNoteJpaTest {

    @Autowired
    private EntityManager em;

    @Test
    @Transactional
    void deleteParentCascadesToChildren() {
        Game game = new Game();
        game.setName("TestGame");
        game.setPlayed(false);

        // root note
        GameNote root = new GameNote();
        root.setContent("Root");
        root.setDate(LocalDateTime.now());
        root.setAuthorUsername("user1");

        // child note (reply)
        GameNote reply = new GameNote();
        reply.setContent("Reply");
        reply.setDate(LocalDateTime.now());
        reply.setAuthorUsername("user2");
        reply.setParent(root);

        // wire children
        List<GameNote> notes = new ArrayList<>();
        notes.add(root);
        notes.add(reply);

        root.setChildren(List.of(reply));

        game.setNotes(notes);

        em.persist(game);
        em.flush();

        // Fetch ids
        Long replyId = reply.getId();
        assertThat(replyId).isNotNull();

        // Now remove root from the collection and merge
        game.getNotes().remove(root);
        em.merge(game);
        em.flush();

        // reply should be removed due to orphanRemoval cascade
        GameNote found = em.find(GameNote.class, replyId);
        assertThat(found).isNull();
    }
}
