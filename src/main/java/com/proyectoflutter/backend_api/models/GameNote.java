package com.proyectoflutter.backend_api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

// @JsonFormat fue eliminado intencionalmente.
// Jackson deserializa LocalDateTime desde strings ISO-8601 de forma nativa
// gracias a JavaTimeModule + las propiedades definidas en application.properties.
// Esto acepta cualquier variante ISO-8601: con o sin microsegundos.
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameNote {

    private String content;
    private LocalDateTime date;
    private String authorUsername;
 
    public GameNote() {}
 
    public GameNote(String content, LocalDateTime date, String authorUsername) {
        this.content = content;
        this.date = date;
        this.authorUsername = authorUsername;
    }
 
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
}