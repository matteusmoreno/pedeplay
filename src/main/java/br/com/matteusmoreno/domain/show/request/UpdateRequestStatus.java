package br.com.matteusmoreno.domain.show.request;

import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRequestStatus(
        @NotNull(message = "Status cannot be null")
        RequestStatus status
) {}