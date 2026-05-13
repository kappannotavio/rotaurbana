package io.github.uri.rotaurbana.dto.response;

public record LoginResponseDTO(
        String token,
        Long userId,
        Long driverId,
        String role
) {
}
