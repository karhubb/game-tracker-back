package com.proyectoflutter.backend_api.models;

import jakarta.persistence.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "juegos")
public class Game {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private static final class GameNotePayload {
            public String content;
            public String date;
            public String authorUsername;
        }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String coverUrl;

    private String franchise;
    private String category;

    @Column(nullable = false)
    private Integer rating = 5;

    @Column(columnDefinition = "TEXT")
    private String notesJson = "[]";

    @Transient
    private List<GameNote> notes = new ArrayList<>();
    
    public Game() {
        this.notesJson = "[]";
        this.notes = new ArrayList<>();
    }

    @PrePersist
    @PreUpdate
    public void serializeNotes() {
        try {
            List<GameNotePayload> payloads = new ArrayList<>();
            if (this.notes != null && !this.notes.isEmpty()) {
                for (GameNote note : this.notes) {
                    GameNotePayload payload = new GameNotePayload();
                    payload.content = note.getContent();
                    payload.date = note.getDate() == null ? null : note.getDate().toString();
                    payload.authorUsername = note.getAuthorUsername();
                    payloads.add(payload);
                }
            }
            this.notesJson = MAPPER.writeValueAsString(payloads);
        } catch (Exception e) {
            this.notesJson = "[]";
        }
    }

    @PostLoad
    private void deserializeNotes() {
        try {
            if (this.notesJson != null && !this.notesJson.isEmpty() && !this.notesJson.equals("[]")) {
                List<GameNotePayload> payloads = MAPPER.readValue(
                        this.notesJson,
                        new TypeReference<List<GameNotePayload>>() {});
                List<GameNote> parsed = new ArrayList<>();
                for (GameNotePayload payload : payloads) {
                    java.time.LocalDateTime parsedDate = null;
                    if (payload.date != null && !payload.date.isBlank()) {
                        parsedDate = java.time.LocalDateTime.parse(payload.date);
                    }
                    parsed.add(new GameNote(payload.content, parsedDate, payload.authorUsername));
                }
                this.notes = parsed;
            } else {
                this.notes = new ArrayList<>();
            }
        } catch (Exception e) {
            this.notes = new ArrayList<>();
        }
    }

    @Column(nullable = false)
    private Boolean played;

    public List<GameNote> getNotes() { return notes; }
    public void setNotes(List<GameNote> notes) { 
        this.notes = notes;

        serializeNotes();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getFranchise() { return franchise; }
    public void setFranchise(String franchise) { this.franchise = franchise; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Boolean getPlayed() { return played; }
    public void setPlayed(Boolean played) { this.played = played; }
    public String getNotesJson() { return notesJson; }
    public void setNotesJson(String notesJson) { this.notesJson = notesJson; }
}