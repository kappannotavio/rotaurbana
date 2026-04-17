package io.github.uri.rotaurbana.entity;

import io.github.uri.rotaurbana.enums.Role;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String adress;
    private String city;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER; //Valor Default

    private String email;
    private String password;
    private LocalDate birthDate;

    //Construtor

    public UserEntity(String fullName, String adress, String city, String email, String password, LocalDate birthDate) {
        this.fullName = fullName;
        this.adress = adress;
        this.city = city;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.role == Role.ADMIN) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        else return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
