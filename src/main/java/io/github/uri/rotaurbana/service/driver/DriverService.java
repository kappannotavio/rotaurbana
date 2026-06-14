package io.github.uri.rotaurbana.service.driver;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import io.github.uri.rotaurbana.entity.*;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import io.github.uri.rotaurbana.service.tracking.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DriverService {

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @Autowired
    private TrackingService trackingService;

    public DriverEntity validateAndGetDriver(UserEntity user, Long driverId) {
        if (user.getRole() != Role.DRIVER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas motoristas podem enviar localização");

        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Motorista não encontrado"));

        if (driver.getUser() == null || !driver.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        return driver;
    }

    public void sendLocation(DriverEntity driver, Long busId, LocationRequestDTO dto) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        if (bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista deste ônibus");

        trackingService.updateBusLocation(busId, dto);
    }

    public void sendRouteLocation(DriverEntity driver, Long routeId, LocationRequestDTO dto) {
        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        BusEntity bus = route.getBus();
        if (bus == null || bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista desta rota");

        trackingService.updateRouteLocation(routeId, dto);
    }

    public List<Map<String, Object>> getMyBuses(DriverEntity driver) {
        return busRepository.findByDriver(driver).stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idBus", b.getIdBus());
            m.put("sign", b.getSign());
            m.put("model", b.getModel());
            m.put("brand", b.getBrand());
            m.put("color", b.getColor());
            m.put("code", b.getCode());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBusRoutes(DriverEntity driver, Long busId) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        if (bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista deste ônibus");

        return routesRepository.findByBus(bus).stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departurePoint", r.getDeparturePoint());
            m.put("departureAddress", r.getDepartureAddress());
            m.put("destinationAddress", r.getDestinationAddress());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            m.put("estimatedDuration", r.getEstimatedDuration());
            m.put("departureLatitude", r.getDepartureLatitude());
            m.put("departureLongitude", r.getDepartureLongitude());
            m.put("destinationLatitude", r.getDestinationLatitude());
            m.put("destinationLongitude", r.getDestinationLongitude());
            m.put("code", r.getCode());
            return m;
        }).collect(Collectors.toList());
    }
}
