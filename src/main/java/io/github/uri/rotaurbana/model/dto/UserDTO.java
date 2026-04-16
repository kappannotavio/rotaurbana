package io.github.uri.rotaurbana.model.dto;

import io.github.uri.rotaurbana.enums.Role;

import java.util.Date;

public class UserDTO {

    private Long idUser;
    private String fullName;
    private String adress;
    private String city;
    private Role role;
    private String login;
    private String password;
    private Date birthday;

    public UserDTO(String fullName, String adress, String city, Role role, String login, String password, Date birthday) {
        this.fullName = fullName;
        this.adress = adress;
        this.city = city;
        this.role = role;
        this.login = login;
        this.password = password;
        this.birthday = birthday;
    }


}
