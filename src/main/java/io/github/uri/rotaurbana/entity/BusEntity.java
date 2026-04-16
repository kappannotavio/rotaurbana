package io.github.uri.rotaurbana.entity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "bus")
public class BusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idBus;

    private String brand;
    private String model;
    private String color;
    private String sign;
    private double mileage;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkIdDriver")
    private DriverEntity driver;
}
