package br.com.matteusmoreno.domain.song;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@MongoEntity(collection="songs")
@Builder
public class Song extends PanacheMongoEntity {
    public String title;
    public String artistName;
    public LocalDateTime createdAt;
}
