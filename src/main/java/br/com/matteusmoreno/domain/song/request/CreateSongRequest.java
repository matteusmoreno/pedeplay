package br.com.matteusmoreno.domain.song.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSongRequest(
        @NotBlank(message = "Title is required")
        String title,
        @NotBlank(message = "Artist name is required")
        String artistName) {}
