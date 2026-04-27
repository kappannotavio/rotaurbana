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

    private String busImageUrl;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkIdDriver")
    private DriverEntity driver;


    //Constructors
    public BusEntity(DriverEntity driver, String busImageUrl, double mileage, String sign, String color, String model, String brand) {
        this.driver = driver;
        this.busImageUrl = busImageUrl;
        this.mileage = mileage;
        this.sign = sign;
        this.color = color;
        this.model = model;
        this.brand = brand;
    }

    public BusEntity() {}

}
