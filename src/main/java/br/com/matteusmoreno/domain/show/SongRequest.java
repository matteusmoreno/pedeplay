package br.com.matteusmoreno.domain.show;

import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SongRequest {
    public ObjectId requestId;
    public String songTitle;
    public String songArtist;
    public BigDecimal tipAmount;
    public String clientMessage;
    public RequestStatus status;
    public ObjectId paymentId;
    public LocalDateTime receivedAt;
}
