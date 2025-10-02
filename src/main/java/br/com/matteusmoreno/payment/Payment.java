package br.com.matteusmoreno.payment;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@MongoEntity(collection="payments")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment extends PanacheMongoEntity {
    public ObjectId showEventId;
    public ObjectId artistId;
    public BigDecimal amount;
    public String currency;
    public PaymentType type;
    public String gateway;
    public String transactionId;
    public PaymentStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}