package io.github.uri.rotaurbana.dto.request;

import java.time.LocalDate;

public record RegisterRequestDTO(
        String fullName,
        String adress,
        String city,
        String email,
        String password,
        LocalDate birthDate,
        String userImageUrl
) {
}
