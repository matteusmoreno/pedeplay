package br.com.matteusmoreno.domain.song;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class SongService {

    public Song createSong(CreateSongRequest request) {
        //se a musica existir, nao crie , apenas retorne a existente

        Song song = Song.builder()
                .title(request.title().trim())
                .artistName(request.artistName().toUpperCase())
                .createdAt(LocalDateTime.now())
                .build();

        song.persist();
        return song;
    }
}
