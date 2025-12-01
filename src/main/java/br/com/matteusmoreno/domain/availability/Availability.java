package br.com.matteusmoreno.domain.availability;

import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@MongoEntity(collection = "availability")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Availability extends PanacheMongoEntity {

  public ObjectId artistId;
  public LocalDateTime startTime;
  public LocalDateTime endTime;
  public AvailabilityStatus availabilityStatus;
  public BigDecimal price;

  public static Availability findAvailabilityById(String id) {
      return findById(new ObjectId(id));
  }

  public static List<Availability> findAllAvailabilitiesByArtist(ObjectId artistId) {
      return list("artistId", artistId);
  }

}
