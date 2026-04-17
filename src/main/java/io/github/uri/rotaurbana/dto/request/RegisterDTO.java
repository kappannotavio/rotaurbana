package io.github.uri.rotaurbana.dto.request;

import java.time.LocalDate;

public record RegisterDTO(String fullName, String adress, String city, String email, String password, LocalDate birthDate) {
}
