package br.com.matteusmoreno.domain.artist;

import br.com.matteusmoreno.domain.subscription.Subscription;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@MongoEntity(collection="artists")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Artist extends PanacheMongoEntity {

    public ObjectId artistId;
    public String name;
    public String email;
    public String password;
    public String profileImageUrl;
    public List<ObjectId> repertoire;
    public String profileQrCodeUrl;
    public SocialLinks socialLinks;
    public Boolean active;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime deletedAt;
    public Subscription subscription;

}