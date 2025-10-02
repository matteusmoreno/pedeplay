package br.com.matteusmoreno.domain.show;

import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@MongoEntity(collection="showEvents")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowEvent extends PanacheMongoEntity {
    public ObjectId artistId;
    public ShowStatus status;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public Integer durationInSeconds;
    public BigDecimal totalTipsValue;
    public Integer totalRequests;
    public List<SongRequest> requests;
}
