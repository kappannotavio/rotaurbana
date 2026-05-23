package io.github.uri.rotaurbana.dto.request;

import java.sql.Time;

public record RoutesRequestDTO(
        Time destinationTime,
        Time departureTime,
        String destiny,
        Long fkIdBus
) {
}
