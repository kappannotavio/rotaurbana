package io.github.uri.rotaurbana.dto.request;

public class DriverDTO {
    private Long idDriver;
    private String licence;
    private Long fkIdUser;

    public DriverDTO(String licence, Long fkIdUser) {
        this.licence = licence;
        this.fkIdUser = fkIdUser;
    }
}
