package io.github.uri.rotaurbana.entity;

import io.github.uri.rotaurbana.enums.PaymentStatus;
import io.github.uri.rotaurbana.enums.Role;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.Collection;
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
    private String userImageUrl;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.EM_DAY;

    //Construtor
    public UserEntity(String fullName, String adress, String city, String email, String password, LocalDate birthDate, String userImageUrl) {
        this.fullName = fullName;
        this.adress = adress;
        this.city = city;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.userImageUrl = (userImageUrl == null || userImageUrl.isBlank()) ? "padrao" : userImageUrl; //NAO ESQUECE DE MUDAR O DEFAULT
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == Role.ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
