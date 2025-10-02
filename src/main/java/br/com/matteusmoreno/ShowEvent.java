package br.com.matteusmoreno;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@MongoEntity(collection="showEvents")
public class ShowEvent extends PanacheMongoEntity {
    public ObjectId artistId;
    public ShowStatus status;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public BigDecimal totalTipsValue;
    public List<SongRequest> requests;
}
