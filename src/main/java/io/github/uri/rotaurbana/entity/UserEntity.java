package io.github.uri.rotaurbana.entity;

import io.github.uri.rotaurbana.enums.Role;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name =  "user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String adress;
    private String city;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String login;
    private String password;
    private Date birthday;

}
