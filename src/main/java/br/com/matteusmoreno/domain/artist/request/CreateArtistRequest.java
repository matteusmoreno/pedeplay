package br.com.matteusmoreno.domain.artist.request;

import br.com.matteusmoreno.domain.artist.SocialLinks;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.bson.types.ObjectId;

import java.util.List;

public record CreateArtistRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "Biography is required")
        String biography,
        @NotBlank(message = "Profile image URL is required")
        String profileImageUrl,
        @NotBlank(message = "CEP is required")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "Invalid CEP format")
        String cep,
        @NotBlank(message = "Number is required")
        String number,
        String complement,
        SocialLinks socialLinks) {}
