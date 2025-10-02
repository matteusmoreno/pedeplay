package br.com.matteusmoreno.domain.artist.service;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.request.AddSongRequest;
import br.com.matteusmoreno.domain.artist.request.CreateArtistRequest;
import br.com.matteusmoreno.domain.subscription.service.PlanService;
import br.com.matteusmoreno.domain.subscription.service.SubscriptionService;
import br.com.matteusmoreno.exception.ArtistNotFoundException;
import br.com.matteusmoreno.exception.EmailAlreadyExistsException;
import br.com.matteusmoreno.exception.SubscriptionLimitExceededException;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArtistService {

    private final SubscriptionService subscriptionService;
    private final PlanService planService;


    public ArtistService(SubscriptionService subscriptionService, PlanService planService) {
        this.subscriptionService = subscriptionService;
        this.planService = planService;
    }

    public Artist createArtist(CreateArtistRequest request) {
        Artist.find("email", request.email()).firstResultOptional().ifPresent(artist -> {
            throw new EmailAlreadyExistsException("Email '" + request.email() + "' is already in use.");
        });

        Artist artist = Artist.builder()
                .name(request.name())
                .email(request.email())
                .emailVerified(false)
                .password(request.password()) // In a real application, ensure to hash the password
                .biography(request.biography())
                .balance(BigDecimal.ZERO)
                .profileImageUrl(request.profileImageUrl())
                .profileQrCodeUrl("")
                .socialLinks(request.socialLinks())
                .subscription(subscriptionService.createFreeSubscription())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        artist.persist();
        return artist;
    }

    public Artist addSongsToRepertoire(AddSongRequest request) {
        Artist artist = Artist.findById(request.artistId());
        if (artist == null) throw new ArtistNotFoundException("Artist not found with ID: " + request.artistId());

        Integer songLimit = planService.getSongLimitForPlan(artist.subscription.planType);
        int currentRepertoireSize = artist.repertoire.size();

        if (currentRepertoireSize + request.songIds().size() > songLimit) {
            throw new SubscriptionLimitExceededException("Cannot add songs. Repertoire limit of " + songLimit + " for plan " + artist.subscription.planType + " will be exceeded.");
        }

        artist.repertoire.addAll(request.songIds());
        artist.update();

        return artist;
    }
}