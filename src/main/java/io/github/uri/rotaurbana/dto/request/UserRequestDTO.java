package io.github.uri.rotaurbana.dto.request;

import io.github.uri.rotaurbana.enums.Role;

import java.time.LocalDate;

public record UserRequestDTO(
        String fullName,
        String adress,
        String city,
        Role role,
        String email,
        String password,
        LocalDate birthday,
        String userImageUrl
) {
}
