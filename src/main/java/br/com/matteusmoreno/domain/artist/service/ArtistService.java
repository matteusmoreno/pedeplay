package br.com.matteusmoreno.domain.artist.service;

import br.com.matteusmoreno.domain.address.Address;
import br.com.matteusmoreno.domain.address.AddressService;
import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.request.AddSongRequest;
import br.com.matteusmoreno.domain.artist.request.CreateArtistRequest;
import br.com.matteusmoreno.domain.artist.request.UpdateArtistRequest;
import br.com.matteusmoreno.domain.subscription.service.PlanService;
import br.com.matteusmoreno.domain.subscription.service.SubscriptionService;
import br.com.matteusmoreno.exception.*;
import br.com.matteusmoreno.infrastructure.viacep.ViaCepClient;
import br.com.matteusmoreno.infrastructure.viacep.ViaCepResponse;
import br.com.matteusmoreno.security.SecurityService;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ArtistService {

    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final SecurityService securityService;
    private final AddressService addressService;


    public ArtistService(SubscriptionService subscriptionService, PlanService planService, SecurityService securityService, AddressService addressService) {
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.securityService = securityService;
        this.addressService = addressService;
    }

    // CREATE A NEW ARTIST
    public Artist createArtist(CreateArtistRequest request) {
        Artist.find("email", request.email()).firstResultOptional().ifPresent(artist -> {
            throw new EmailAlreadyExistsException("Email '" + request.email() + "' is already in use.");
        });

        Address address = addressService.getAddressByCep(request.cep());

        String hashedPassword = securityService.hashPassword(request.password());

        Artist artist = Artist.builder()
                .name(request.name())
                .email(request.email())
                .emailVerified(false)
                .password(hashedPassword)
                .biography(request.biography())
                .balance(BigDecimal.ZERO)
                .profileImageUrl(request.profileImageUrl())
                .profileQrCodeUrl("")
                .address(address)
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

    // GET ARTIST BY ID
    public Artist getArtistById(ObjectId artistId) {
        Artist artist = Artist.findById(artistId);
        if (artist == null) throw new ArtistNotFoundException("Artist not found with ID: " + artistId);

        return artist;
    }

    // GET ALL ARTISTS WITH PAGINATION
    public List<Artist> getAllArtists(int page, int size) {
        return Artist.findAll(Sort.ascending("name")).page(page, size).list();
    }

    // UPDATE ARTIST DETAILS

    // DISABLE ARTIST ACCOUNT
    public void disableArtist(ObjectId artistId) {
        Artist artist = getArtistById(artistId);

        if (!artist.active) throw new ArtistAlreadyDisabledException("Artist with ID: " + artistId + " is already disabled.");

        artist.active = false;
        artist.updatedAt = LocalDateTime.now();
        artist.deletedAt = LocalDateTime.now();

        artist.update();
    }

    // ENABLE ARTIST ACCOUNT
    public void enableArtist(ObjectId artistId) {
        Artist artist = getArtistById(artistId);

        if (artist.active) throw new ArtistAlreadyEnabledException("Artist with ID: " + artistId + " is already enabled.");

        artist.active = true;
        artist.updatedAt = LocalDateTime.now();
        artist.deletedAt = null;

        artist.update();
    }

    // ADD SONGS TO ARTIST'S REPERTOIRE WITH SUBSCRIPTION LIMIT CHECK
    public Artist addSongsToRepertoire(AddSongRequest request) {
        Artist artist = getArtistById(request.artistId());

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