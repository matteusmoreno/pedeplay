package br.com.matteusmoreno.domain.contract;

import br.com.matteusmoreno.domain.contract.constant.ContractStatus;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "contracts")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contract extends PanacheMongoEntity {

  public ObjectId artistId;
  public ObjectId customerId;
  public List<ObjectId> availabilityIds;
  public BigDecimal totalPrice;
  public ContractStatus contractStatus;
  public LocalDateTime createdAt;
  public LocalDateTime confirmedAt;
  public LocalDateTime rejectedAt;
  public LocalDateTime canceledAt;
  public LocalDateTime completedAt;

  public static List<Contract> findAllByArtistId(ObjectId artistId) {
    return list("artistId", artistId);
  }

}
