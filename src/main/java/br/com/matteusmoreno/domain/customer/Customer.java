package br.com.matteusmoreno.domain.customer;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@MongoEntity(collection = "customers")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer extends PanacheMongoEntity {

  public String name;
  public String phoneNumber;
  public String email;
  public String documentNumber;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public static Customer findByPhoneNumberOrEmailOrDocumentNumber(String phoneNumber, String email, String documentNumber) {
    return find("phoneNumber = ?1 or email = ?2 or documentNumber = ?3", phoneNumber, email, documentNumber).firstResult();
  }

}
