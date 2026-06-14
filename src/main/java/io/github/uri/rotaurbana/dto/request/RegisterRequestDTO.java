package io.github.uri.rotaurbana.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome completo é obrigatório")
        String fullName,

        @NotBlank(message = "Endereço é obrigatório")
        String adress,

        @NotBlank(message = "Cidade é obrigatória")
        String city,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @NotBlank(message = "Confirmação de senha é obrigatória")
        String confirmPassword,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate birthDate,

        String userImageUrl
) {
}
