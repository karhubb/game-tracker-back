package com.proyectoflutter.backend_api.payload.response;

import java.util.LinkedHashMap;
import java.util.Map;

public class NoteReactionSummaryDTO {

    private Long gameId;
    private Integer noteIndex;
    private long total;
    private Map<String, Long> counts = new LinkedHashMap<>();
    private NoteReactionResponseDTO myReaction;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Integer getNoteIndex() {
        return noteIndex;
    }

    public void setNoteIndex(Integer noteIndex) {
        this.noteIndex = noteIndex;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Map<String, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }

    public NoteReactionResponseDTO getMyReaction() {
        return myReaction;
    }

    public void setMyReaction(NoteReactionResponseDTO myReaction) {
        this.myReaction = myReaction;
    }
}
