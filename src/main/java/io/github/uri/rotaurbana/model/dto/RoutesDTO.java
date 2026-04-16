package io.github.uri.rotaurbana.model.dto;

import java.sql.Time;

public class RoutesDTO {

    private Long idRoute;
    private Time destinationTime;
    private Time departureTime;
    private String destiny;
    private Long fkIdBus;

    public RoutesDTO(Time destinationTime, Time departureTime, String destiny, Long fkIdBus) {
        this.destinationTime = destinationTime;
        this.departureTime = departureTime;
        this.destiny = destiny;
        this.fkIdBus = fkIdBus;
    }
}
