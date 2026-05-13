package io.github.uri.rotaurbana.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "routes")
public class RoutesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRoute;

    private LocalTime destinationTime;
    private LocalTime departureTime;
    private String destiny;
    private String departurePoint;
    private String departureAddress;
    private String destinationAddress;
    private String estimatedDuration;
    private Double departureLatitude;
    private Double departureLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;

    @Column(unique = true, length = 6)
    private String code;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_id_bus")
    private BusEntity bus;

    @ManyToMany
    @JoinTable(
        name = "route_passengers",
        joinColumns = @JoinColumn(name = "route_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> passengers = new HashSet<>();

}
