package br.com.matteusmoreno.domain.show.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StartLiveStreamRequest {

    @NotBlank(message = "Show ID is required")
    private String showId;

    @Pattern(regexp = "SD|HD|FHD", message = "Stream quality must be SD, HD, or FHD")
    private String streamQuality = "HD";
}

