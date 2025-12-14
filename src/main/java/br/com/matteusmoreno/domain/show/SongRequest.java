package br.com.matteusmoreno.domain.show;

import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SongRequest {

    public ObjectId requestId;
    public String songTitle;
    public String songArtist;
    public BigDecimal tipAmount;
    public String clientMessage;
    public RequestStatus status;
    public String paymentId;
    public LocalDateTime receivedAt;
}
