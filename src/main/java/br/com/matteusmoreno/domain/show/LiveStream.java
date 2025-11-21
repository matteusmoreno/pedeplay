package br.com.matteusmoreno.domain.show;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma transmissão ao vivo
 */
@MongoEntity(collection="liveStreams")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class LiveStream extends PanacheMongoEntity {

    public ObjectId showId;
    public ObjectId artistId;
    public Boolean isActive;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public Integer totalViewers; // Total de viewers únicos
    public Integer peakViewers; // Pico de viewers simultâneos
    public String streamQuality; // Qualidade do stream (SD, HD, FHD)

  public static LiveStream findActiveByShowId(ObjectId showId) {
    log.info("🔎 Executandooo query: showId = {} and isActive = true", showId);

    // Tenta com sintaxe MongoDB nativa
    LiveStream result = find("{'showId': ?1, 'isActive': true}", showId).firstResult();

    log.info("📋 Query retornou: {}", result != null ? "SUCESSO" : "NULL");
    return result;
  }

    public static LiveStream findActiveByArtistId(ObjectId artistId) {
        return find("artistId = ?1 and isActive = true", artistId).firstResult();
    }
}

