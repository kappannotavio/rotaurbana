package io.github.uri.rotaurbana.dto.request;

import io.github.uri.rotaurbana.enums.Role;

import java.time.LocalDate;

public class UserDTO {

    private Long idUser;
    private String fullName;
    private String adress;
    private String city;
    private Role role;
    private String email;
    private String password;
    private LocalDate birthday;

    public UserDTO(String fullName, String adress, String city, Role role, String email, String password, LocalDate birthday) {
        this.fullName = fullName;
        this.adress = adress;
        this.city = city;
        this.role = role;
        this.email = email;
        this.password = password;
        this.birthday = birthday;
    }


}
