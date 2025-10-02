package br.com.matteusmoreno.domain.artist.request;

import br.com.matteusmoreno.domain.artist.SocialLinks;
import org.bson.types.ObjectId;

public record UpdateArtistRequest(
        ObjectId id,
        String name,
        String biography,
        String email,
        String cep,
        String number,
        String complement,
        SocialLinks socialLinks) {}
