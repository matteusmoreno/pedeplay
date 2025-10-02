package br.com.matteusmoreno.api.websocket.dto;

import br.com.matteusmoreno.domain.show.SongRequest;

public record NewSongRequestNotification(
        String type,
        SongRequest data
) {
    public NewSongRequestNotification(SongRequest songRequest) {
        this("NEW_SONG_REQUEST", songRequest);
    }
}