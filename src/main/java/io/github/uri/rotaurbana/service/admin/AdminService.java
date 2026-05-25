package io.github.uri.rotaurbana.service.admin;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.*;
import io.github.uri.rotaurbana.enums.PaymentStatus;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.*;
import io.github.uri.rotaurbana.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

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

    public void validateAdmin(UserEntity admin, Long adminId) {
        if (admin.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores");
        if (!admin.getId().equals(adminId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    public List<Map<String, Object>> listBuses() {
        return busRepository.findAll().stream().map(b -> {
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
    }

    public List<Map<String, Object>> listRoutes() {
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
        return result;
    }

    public RoutesEntity createRoute(Map<String, String> body) {
        String busIdStr = body.get("busId");
        String destiny = body.get("destiny");
        String departureTime = body.get("departureTime");
        String destinationTime = body.get("destinationTime");
        String departurePoint = body.get("departurePoint");
        String departureAddress = body.get("departureAddress");
        String destinationAddress = body.get("destinationAddress");
        String estimatedDuration = body.get("estimatedDuration");
        String departureLatStr = body.get("departureLatitude");
        String departureLngStr = body.get("departureLongitude");
        String destinationLatStr = body.get("destinationLatitude");
        String destinationLngStr = body.get("destinationLongitude");

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
        if (departureLatStr != null) route.setDepartureLatitude(Double.parseDouble(departureLatStr));
        if (departureLngStr != null) route.setDepartureLongitude(Double.parseDouble(departureLngStr));
        if (destinationLatStr != null) route.setDestinationLatitude(Double.parseDouble(destinationLatStr));
        if (destinationLngStr != null) route.setDestinationLongitude(Double.parseDouble(destinationLngStr));
        route.setDepartureTime(LocalTime.parse(departureTime, DateTimeFormatter.ofPattern("HH:mm")));
        route.setDestinationTime(LocalTime.parse(destinationTime, DateTimeFormatter.ofPattern("HH:mm")));
        route.setCode(busService.generateUniqueRouteCode());
        return routesRepository.save(route);
    }

    public List<Map<String, Object>> listDrivers() {
        return driverRepository.findAll().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idDriver", d.getIdDriver());
            m.put("licence", d.getLicence());
            m.put("fullName", d.getUser() != null ? d.getUser().getFullName() : "Sem usuário");
            m.put("email", d.getUser() != null ? d.getUser().getEmail() : "");
            return m;
        }).collect(Collectors.toList());
    }

    public void registerBus(BusRequestDTO dto) {
        if (dto.driverId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId é obrigatório");

        DriverEntity driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));

        busService.registerBus(dto, driver);
    }

    public void createDriver(Map<String, String> body) {
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
    }

    public Map<String, Object> getRouteDetails(Long routeId) {
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

            if (p.getPaymentStatus() == PaymentStatus.EM_DAY) emDayCount++;
            else if (p.getPaymentStatus() == PaymentStatus.PENDING) pendingCount++;
            else if (p.getPaymentStatus() == PaymentStatus.LATE) lateCount++;

            passengerList.add(pm);
        }
        response.put("passengers", passengerList);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPassengers", passengers.size());
        stats.put("emDayCount", emDayCount);
        stats.put("pendingCount", pendingCount);
        stats.put("lateCount", lateCount);
        response.put("stats", stats);

        return response;
    }

    public void updatePayments(Long routeId, List<Map<String, Object>> payments) {
        routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        for (Map<String, Object> entry : payments) {
            Long userId = Long.valueOf(entry.get("userId").toString());
            String statusStr = (String) entry.get("paymentStatus");

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId));

            user.setPaymentStatus(PaymentStatus.valueOf(statusStr));
            userRepository.save(user);
        }
    }
}
