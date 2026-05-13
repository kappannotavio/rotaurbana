package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class BusService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @Autowired
    private DriverRepository driverRepository;

    public BusEntity registerBus(BusRequestDTO dto, DriverEntity driver) {
        if (busRepository.findBySign(dto.sign()) != null)
            throw new RuntimeException("Já existe um ônibus com essa placa");

        BusEntity bus = new BusEntity(
                driver,
                dto.busImageUrl(),
                dto.mileage(),
                dto.sign(),
                dto.color(),
                dto.model(),
                dto.brand()
        );

        bus.setCode(generateUniqueCode());

        return busRepository.save(bus);
    }

    public String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (busRepository.findByCode(code) != null);
        return code;
    }

    public String generateUniqueRouteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (routesRepository.findByCode(code) != null);
        return code;
    }

}
