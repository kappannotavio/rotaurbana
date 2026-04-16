package io.github.uri.rotaurbana.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "routes")
public class RoutesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRoute;

    private LocalTime destinationTime;
    private LocalTime departureTime;
    private String destiny;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkIdBus")
    private BusEntity bus;

}
