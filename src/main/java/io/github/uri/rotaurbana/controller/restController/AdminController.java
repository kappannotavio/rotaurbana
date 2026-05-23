package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.*;
import io.github.uri.rotaurbana.enums.PaymentStatus;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.PresenceRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private PresenceRepository presenceRepository;

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
            m.put("color", b.getColor());
            m.put("code", b.getCode());
            m.put("mileage", b.getMileage());
            m.put("busImageUrl", b.getBusImageUrl());
            m.put("driverName", b.getDriver() != null && b.getDriver().getUser() != null
                    ? b.getDriver().getUser().getFullName() : "Sem motorista");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> listRoutes(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<RoutesEntity> routes = routesRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (RoutesEntity r : routes) {
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departurePoint", r.getDeparturePoint());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            m.put("estimatedDuration", r.getEstimatedDuration());
            m.put("code", r.getCode());

            BusEntity bus = r.getBus();
            if (bus != null) {
                m.put("busSign", bus.getSign());
                m.put("busModel", bus.getModel());
                m.put("busBrand", bus.getBrand());
                if (bus.getDriver() != null && bus.getDriver().getUser() != null) {
                    m.put("driverName", bus.getDriver().getUser().getFullName());
                }
            }

            Set<UserEntity> routePassengers = r.getPassengers();
            int total = routePassengers.size();
            int emDay = 0, pending = 0, late = 0;
            for (UserEntity p : routePassengers) {
                if (p.getPaymentStatus() == PaymentStatus.EM_DAY) emDay++;
                else if (p.getPaymentStatus() == PaymentStatus.PENDING) pending++;
                else if (p.getPaymentStatus() == PaymentStatus.LATE) late++;
            }
            m.put("totalPassengers", total);
            m.put("emDayCount", emDay);
            m.put("pendingCount", pending);
            m.put("lateCount", late);

            boolean hasDriver = bus != null && bus.getDriver() != null;
            boolean hasPassengers = total > 0;
            m.put("status", hasDriver && hasPassengers ? "ativa" : "pendente");

            result.add(m);
        }

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

    @GetMapping("/routes/{routeId}/details")
    public ResponseEntity<Map<String, Object>> getRouteDetails(Authentication auth, @PathVariable Long adminId, @PathVariable Long routeId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        Map<String, Object> response = new HashMap<>();

        Map<String, Object> routeData = new HashMap<>();
        routeData.put("idRoute", route.getIdRoute());
        routeData.put("destiny", route.getDestiny());
        routeData.put("departurePoint", route.getDeparturePoint());
        routeData.put("departureAddress", route.getDepartureAddress());
        routeData.put("destinationAddress", route.getDestinationAddress());
        routeData.put("departureTime", route.getDepartureTime() != null ? route.getDepartureTime().toString() : null);
        routeData.put("destinationTime", route.getDestinationTime() != null ? route.getDestinationTime().toString() : null);
        routeData.put("estimatedDuration", route.getEstimatedDuration());
        routeData.put("departureLatitude", route.getDepartureLatitude());
        routeData.put("departureLongitude", route.getDepartureLongitude());
        routeData.put("destinationLatitude", route.getDestinationLatitude());
        routeData.put("destinationLongitude", route.getDestinationLongitude());
        routeData.put("code", route.getCode());
        response.put("route", routeData);

        BusEntity bus = route.getBus();
        if (bus != null) {
            Map<String, Object> busData = new HashMap<>();
            busData.put("idBus", bus.getIdBus());
            busData.put("sign", bus.getSign());
            busData.put("brand", bus.getBrand());
            busData.put("model", bus.getModel());
            busData.put("color", bus.getColor());
            busData.put("code", bus.getCode());
            busData.put("busImageUrl", bus.getBusImageUrl());
            response.put("bus", busData);

            DriverEntity driver = bus.getDriver();
            if (driver != null && driver.getUser() != null) {
                Map<String, Object> driverData = new HashMap<>();
                driverData.put("idDriver", driver.getIdDriver());
                driverData.put("fullName", driver.getUser().getFullName());
                driverData.put("licence", driver.getLicence());
                driverData.put("email", driver.getUser().getEmail());
                response.put("driver", driverData);
            }
        }

        Set<UserEntity> passengers = route.getPassengers();
        LocalDate today = LocalDate.now();
        List<PresenceEntity> todayPresences = presenceRepository.findByRouteAndDate(route, today);

        Map<Long, String> presenceMap = new HashMap<>();
        for (PresenceEntity p : todayPresences) {
            presenceMap.put(p.getUser().getId(), p.getPresenceType());
        }

        int emDayCount = 0, pendingCount = 0, lateCount = 0;
        List<Map<String, Object>> passengerList = new ArrayList<>();
        for (UserEntity p : passengers) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", p.getId());
            pm.put("fullName", p.getFullName());
            pm.put("paymentStatus", p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null);

            String pt = presenceMap.get(p.getId());
            pm.put("presenceType", pt);
            String label = "";
            if (pt != null) {
                switch (pt) {
                    case "vai_volta": label = "Vai e Volta"; break;
                    case "so_vai":    label = "Só Vai"; break;
                    case "so_volta":  label = "Só Volta"; break;
                    case "nao_vai":   label = "Não Vai"; break;
                }
            }
            pm.put("presenceTypeLabel", label);

            if (p.getPaymentStatus() == PaymentStatus.EM_DAY) {
                emDayCount++;
            } else if (p.getPaymentStatus() == PaymentStatus.PENDING) {
                pendingCount++;
            } else if (p.getPaymentStatus() == PaymentStatus.LATE) {
                lateCount++;
            }

            passengerList.add(pm);
        }
        response.put("passengers", passengerList);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPassengers", passengers.size());
        stats.put("emDayCount", emDayCount);
        stats.put("pendingCount", pendingCount);
        stats.put("lateCount", lateCount);
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/routes/{routeId}/payment")
    public ResponseEntity<Map<String, Object>> updatePayments(Authentication auth, @PathVariable Long adminId, @PathVariable Long routeId, @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();

        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");

        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> payments = (List<Map<String, Object>>) body.get("payments");

        if (payments == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payments é obrigatório");

        for (Map<String, Object> entry : payments) {
            Long userId = Long.valueOf(entry.get("userId").toString());
            String statusStr = (String) entry.get("paymentStatus");

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId));

            user.setPaymentStatus(PaymentStatus.valueOf(statusStr));
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("message", "Pagamentos atualizados com sucesso"));
    }

}
