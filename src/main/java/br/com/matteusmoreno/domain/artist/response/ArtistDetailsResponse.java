package br.com.matteusmoreno.domain.artist.response;

import br.com.matteusmoreno.domain.address.AddressDetailsResponse;
import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.SocialLinks;
import br.com.matteusmoreno.domain.subscription.Subscription;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ArtistDetailsResponse(
        ObjectId id,
        String name,
        String email,
        Boolean emailVerified,
        String biography,
        BigDecimal balance,
        AddressDetailsResponse address,
        String profileImageUrl,
        List<ObjectId> repertoire,
        String profileQrCodeUrl,
        SocialLinks socialLinks,
        Subscription subscription,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ArtistDetailsResponse(Artist artist) {
        this(
                artist.id,
                artist.name,
                artist.email,
                artist.emailVerified,
                artist.biography,
                artist.balance,
                new AddressDetailsResponse(artist.address),
                artist.profileImageUrl,
                artist.repertoire,
                artist.profileQrCodeUrl,
                artist.socialLinks,
                artist.subscription,
                artist.active,
                artist.createdAt,
                artist.updatedAt
        );
    }
}