package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/driver/{driverId}")
public class DriverController {

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @Autowired
    private PassengerController passengerController;

    @PostMapping("/tracking/{busId}")
    public void sendLocation(Authentication auth, @PathVariable Long driverId, @PathVariable Long busId, @RequestBody LocationRequestDTO dto) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (user.getRole() != Role.DRIVER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas motoristas podem enviar localização");

        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Motorista não encontrado"));

        if (driver.getUser() == null || !driver.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        if (bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista deste ônibus");

        passengerController.updateLocation(busId, dto);
    }

    @PostMapping("/routes/{routeId}/tracking")
    public void sendRouteLocation(Authentication auth, @PathVariable Long driverId, @PathVariable Long routeId, @RequestBody LocationRequestDTO dto) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (user.getRole() != Role.DRIVER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas motoristas podem enviar localização");

        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Motorista não encontrado"));

        if (driver.getUser() == null || !driver.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        BusEntity bus = route.getBus();
        if (bus == null || bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista desta rota");

        passengerController.updateRouteLocation(routeId, dto);
    }

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getMyBuses(Authentication auth, @PathVariable Long driverId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Motorista não encontrado"));

        if (driver.getUser() == null || !driver.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<BusEntity> buses = busRepository.findByDriver(driver);

        List<Map<String, Object>> result = buses.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idBus", b.getIdBus());
            m.put("sign", b.getSign());
            m.put("model", b.getModel());
            m.put("brand", b.getBrand());
            m.put("color", b.getColor());
            m.put("code", b.getCode());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/buses/{busId}/routes")
    public ResponseEntity<List<Map<String, Object>>> getBusRoutes(Authentication auth, @PathVariable Long driverId, @PathVariable Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (user.getRole() != Role.DRIVER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas motoristas");

        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Motorista não encontrado"));

        if (driver.getUser() == null || !driver.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        if (bus.getDriver() == null || bus.getDriver().getIdDriver() != driver.getIdDriver())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista deste ônibus");

        List<RoutesEntity> routes = routesRepository.findByBus(bus);

        List<Map<String, Object>> result = routes.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            m.put("code", r.getCode());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

}
