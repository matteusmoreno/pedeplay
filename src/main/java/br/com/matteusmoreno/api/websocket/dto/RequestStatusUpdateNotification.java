package br.com.matteusmoreno.api.websocket.dto;

import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import org.bson.types.ObjectId;

public record RequestStatusUpdateNotification(
        String type,
        String requestId,
        String newStatus
) {
    public RequestStatusUpdateNotification(ObjectId requestId, RequestStatus newStatus) {
        this("REQUEST_STATUS_UPDATED", requestId.toString(), newStatus.toString());
    }
}