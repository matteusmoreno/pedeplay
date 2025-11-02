package br.com.matteusmoreno.domain.artist;

import br.com.matteusmoreno.domain.address.Address;
import br.com.matteusmoreno.domain.artist.response.ArtistRepertoireDetailsResponse;
import br.com.matteusmoreno.domain.subscription.Subscription;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@MongoEntity(collection="artists")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Artist extends PanacheMongoEntity {

    public String name;
    public String email;
    public Boolean emailVerified;
    public String password;
    public Address address;
    public String biography;
    public BigDecimal balance;
    public String profileImageUrl;
    public List<ObjectId> repertoire = new ArrayList<>();
    public String profileQrCodeUrl;
    public SocialLinks socialLinks;
    public Subscription subscription;
    public Boolean active;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime deletedAt;
}