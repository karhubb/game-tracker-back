package com.proyectoflutter.backend_api.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoflutter.backend_api.models.GameNote;

@Service
public class GameNoteService {

    public static final String DELETED_PLACEHOLDER = "El contenido de este comentario se ha eliminado.";

    public boolean isDeletedPlaceholder(GameNote note) {
        if (note == null) {
            return false;
        }

        if (note.isDeleted()) {
            return true;
        }

        String content = note.getContent();
        return content != null && DELETED_PLACEHOLDER.equals(content.trim());
    }

    public void requireNotDeleted(GameNote note) {
        if (isDeletedPlaceholder(note)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No se puede modificar un comentario ya eliminado"
            );
        }
    }
}