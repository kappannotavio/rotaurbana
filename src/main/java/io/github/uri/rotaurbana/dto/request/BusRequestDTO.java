package io.github.uri.rotaurbana.dto.request;

public record BusRequestDTO(
        String brand,
        String model,
        String color,
        String sign,
        double mileage,
        String busImageUrl,
        Long driverId
) {
}
