package com.proyectoflutter.backend_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @JsonIgnore
    private Game game;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime date;

    private String authorUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private GameNote parent;

    // Persisted column: the list-index of the parent note within this game's notes list.
    // Stored in DB so it can be serialized to JSON without needing lazy-loaded collections.
    @Column(name = "parent_index")
    private Integer parentIndex;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<GameNote> children = new ArrayList<>();

    public GameNote() {}

    public GameNote(String content, LocalDateTime date, String authorUsername) {
        this.content = content;
        this.date = date;
        this.authorUsername = authorUsername;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public GameNote getParent() { return parent; }
    public void setParent(GameNote parent) { this.parent = parent; }

    public List<GameNote> getChildren() { return children; }
    public void setChildren(List<GameNote> children) {
        if (this.children == null) this.children = new ArrayList<>();
        if (children == null) {
            this.children.clear();
            return;
        }
        this.children.clear();
        this.children.addAll(children);
    }

    @JsonProperty("parentIndex")
    public void setParentIndex(Integer parentIndex) {
        this.parentIndex = parentIndex;
    }

    @JsonProperty("parentIndex")
    public Integer getParentIndex() {
        return parentIndex;
    }
}