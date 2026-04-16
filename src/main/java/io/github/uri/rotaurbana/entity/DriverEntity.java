package io.github.uri.rotaurbana.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "driver")
public class DriverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idDriver;

    private String licence;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "fkUserId")
    private UserEntity user;
}
