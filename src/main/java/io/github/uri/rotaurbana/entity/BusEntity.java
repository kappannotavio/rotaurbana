package io.github.uri.rotaurbana.entity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
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

    @Column(unique = true, length = 6)
    private String code;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_id_driver")
    private DriverEntity driver;

    @ManyToMany
    @JoinTable(
        name = "bus_passengers",
        joinColumns = @JoinColumn(name = "bus_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> passengers = new HashSet<>();


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
