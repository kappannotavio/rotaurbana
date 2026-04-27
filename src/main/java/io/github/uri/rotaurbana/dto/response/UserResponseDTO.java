package io.github.uri.rotaurbana.dto.response;

import io.github.uri.rotaurbana.enums.Role;

import java.time.LocalDate;

public record UserResponseDTO(
        String fullName,
        String adress,
        String city,
        Role role,
        String email,
        LocalDate birthday,
        String userImageUrl
) {
}
