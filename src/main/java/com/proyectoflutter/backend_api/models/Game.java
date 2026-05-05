package com.proyectoflutter.backend_api.models;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "juegos")
public class Game {
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

@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("id ASC") // Esto ordena las notas por su ID de forma ascendente
private List<GameNote> notes = new ArrayList<>();

@Column(nullable = false)
private Boolean played;

    public List<GameNote> getNotes() { return notes; }
    public void setNotes(List<GameNote> notes) { 
        // Avoid replacing the collection instance to prevent Hibernate orphan-collection errors.
        if (this.notes == null) {
            this.notes = new ArrayList<>();
        }

        // If incoming is null, clear existing collection and return
        if (notes == null) {
            this.notes.clear();
            return;
        }

        // Prepare a fresh structure while keeping the same list instance
        this.notes.clear();
        // First, copy notes and set back-references
        for (int i = 0; i < notes.size(); i++) {
            GameNote incoming = notes.get(i);
            incoming.setGame(this);
            // preserve existing children list instance if present
            if (incoming.getChildren() == null) {
                incoming.setChildren(new ArrayList<>());
            } else {
                incoming.getChildren().clear();
            }

            // set parent by index if provided and valid
            if (incoming.getParent() == null) {
                Integer parentIndex = incoming.getParentIndex();
                if (parentIndex != null && parentIndex >= 0 && parentIndex < notes.size()) {
                    incoming.setParent(notes.get(parentIndex));
                }
            }

            this.notes.add(incoming);
        }

        // Build children relationships using the kept instances
        for (GameNote note : this.notes) {
            GameNote parent = note.getParent();
            if (parent != null && this.notes.contains(parent)) {
                parent.getChildren().add(note);
            }
        }
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

    // Backwards-compatibility helper used by existing tests and codepaths:
    // serialize `notes` into a JSON string similar to the legacy `notesJson` field.
    public String getNotesJson() {
        if (this.notes == null) return null;
        try {
            ObjectMapper m = new ObjectMapper();
            m.registerModule(new JavaTimeModule());
            return m.writeValueAsString(this.notes);
        } catch (Exception e) {
            return null;
        }
    }
    
}