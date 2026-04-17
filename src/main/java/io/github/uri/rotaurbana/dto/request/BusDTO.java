package io.github.uri.rotaurbana.dto.request;

public class BusDTO {

    private Long idBus;
    private String brand;
    private String model;
    private String color;
    private String sign;
    private double mileage;
    private Long fkIdDriver;

    public BusDTO(String brand, String model, String color, String sign, double mileage, Long fkIdDriver) {
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.sign = sign;
        this.mileage = mileage;
        this.fkIdDriver = fkIdDriver;
    }


}
