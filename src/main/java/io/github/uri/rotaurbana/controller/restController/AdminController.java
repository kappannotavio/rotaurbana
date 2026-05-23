package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import io.github.uri.rotaurbana.repository.UserRepository;
import io.github.uri.rotaurbana.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/{adminId}")
public class AdminController {

    @Autowired
    private BusService busService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> listBuses(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<BusEntity> buses = busRepository.findAll();

        List<Map<String, Object>> result = buses.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idBus", b.getIdBus());
            m.put("sign", b.getSign());
            m.put("model", b.getModel());
            m.put("brand", b.getBrand());
            m.put("code", b.getCode());
            m.put("driverName", b.getDriver() != null && b.getDriver().getUser() != null
                    ? b.getDriver().getUser().getFullName() : "Sem motorista");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/routes")
    public ResponseEntity<Map<String, Object>> createRoute(Authentication auth, @PathVariable Long adminId, @RequestBody Map<String, String> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        String busIdStr = body.get("busId");
        String destiny = body.get("destiny");
        String departureTime = body.get("departureTime");
        String destinationTime = body.get("destinationTime");
        String departurePoint = body.get("departurePoint");
        String departureAddress = body.get("departureAddress");
        String destinationAddress = body.get("destinationAddress");
        String estimatedDuration = body.get("estimatedDuration");
        String departureLatitude = body.get("departureLatitude");
        String departureLongitude = body.get("departureLongitude");
        String destinationLatitude = body.get("destinationLatitude");
        String destinationLongitude = body.get("destinationLongitude");

        if (busIdStr == null || destiny == null || departureTime == null || destinationTime == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos os campos são obrigatórios");

        Long busId = Long.parseLong(busIdStr);
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        RoutesEntity route = new RoutesEntity();
        route.setBus(bus);
        route.setDestiny(destiny);
        route.setDeparturePoint(departurePoint);
        route.setDepartureAddress(departureAddress);
        route.setDestinationAddress(destinationAddress);
        route.setEstimatedDuration(estimatedDuration);
        if (departureLatitude != null) route.setDepartureLatitude(Double.parseDouble(departureLatitude));
        if (departureLongitude != null) route.setDepartureLongitude(Double.parseDouble(departureLongitude));
        if (destinationLatitude != null) route.setDestinationLatitude(Double.parseDouble(destinationLatitude));
        if (destinationLongitude != null) route.setDestinationLongitude(Double.parseDouble(destinationLongitude));
        route.setDepartureTime(LocalTime.parse(departureTime, DateTimeFormatter.ofPattern("HH:mm")));
        route.setDestinationTime(LocalTime.parse(destinationTime, DateTimeFormatter.ofPattern("HH:mm")));
        route.setCode(busService.generateUniqueRouteCode());
        routesRepository.save(route);

        return ResponseEntity.ok(Map.of("message", "Rota cadastrada com sucesso", "code", route.getCode()));
    }

    @GetMapping("/drivers")
    public ResponseEntity<List<Map<String, Object>>> listDrivers(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<DriverEntity> drivers = driverRepository.findAll();

        List<Map<String, Object>> result = drivers.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idDriver", d.getIdDriver());
            m.put("licence", d.getLicence());
            m.put("fullName", d.getUser() != null ? d.getUser().getFullName() : "Sem usuário");
            m.put("email", d.getUser() != null ? d.getUser().getEmail() : "");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/buses")
    public ResponseEntity<Map<String, Object>> registerBus(Authentication auth, @PathVariable Long adminId, @RequestBody BusRequestDTO dto) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        if (dto.driverId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId é obrigatório");

        DriverEntity driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));

        busService.registerBus(dto, driver);

        return ResponseEntity.ok(Map.of("message", "Ônibus cadastrado com sucesso"));
    }

    @PostMapping("/drivers")
    public ResponseEntity<Map<String, Object>> createDriver(Authentication auth, @PathVariable Long adminId, @RequestBody Map<String, String> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        String fullName = body.get("fullName");
        String email = body.get("email");
        String password = body.get("password");
        String licence = body.get("licence");
        String adress = body.get("adress");
        String city = body.get("city");

        if (fullName == null || email == null || password == null || licence == null || adress == null || city == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos os campos são obrigatórios");

        if (userRepository.findByEmail(email) != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");

        UserEntity user = new UserEntity(fullName, adress, city, email, new BCryptPasswordEncoder().encode(password), LocalDate.now(), "");
        user.setRole(Role.DRIVER);
        userRepository.save(user);

        DriverEntity driver = new DriverEntity();
        driver.setUser(user);
        driver.setLicence(licence);
        driverRepository.save(driver);

        return ResponseEntity.ok(Map.of("message", "Motorista cadastrado com sucesso"));
    }

}
