package io.github.uri.rotaurbana.service.admin;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.*;
import io.github.uri.rotaurbana.enums.PaymentStatus;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.*;
import io.github.uri.rotaurbana.service.BusService;
import io.github.uri.rotaurbana.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogService logService;

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
            m.put("driverId", b.getDriver() != null ? b.getDriver().getIdDriver() : null);
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
            int emDay = 0, pending = 0;
            for (UserEntity p : routePassengers) {
                if (p.getPaymentStatus() == PaymentStatus.EM_DAY) emDay++;
                else if (p.getPaymentStatus() == PaymentStatus.PENDING) pending++;
            }
            m.put("totalPassengers", total);
            m.put("emDayCount", emDay);
            m.put("pendingCount", pending);

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
        RoutesEntity saved = routesRepository.save(route);

        logService.log("CRIADO", "ROTA", saved.getIdRoute(),
                "Rota criada: " + departurePoint + " → " + destiny + " (código " + saved.getCode() + ")");

        return saved;
    }

    public List<Map<String, Object>> listDrivers() {
        return driverRepository.findAll().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idDriver", d.getIdDriver());
            m.put("licence", d.getLicence());
            m.put("fullName", d.getUser() != null ? d.getUser().getFullName() : "Sem usuário");
            m.put("email", d.getUser() != null ? d.getUser().getEmail() : "");
            m.put("adress", d.getUser() != null ? d.getUser().getAdress() : "");
            m.put("city", d.getUser() != null ? d.getUser().getCity() : "");
            return m;
        }).collect(Collectors.toList());
    }

    public void registerBus(BusRequestDTO dto) {
        if (dto.driverId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverId é obrigatório");

        DriverEntity driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));

        busService.registerBus(dto, driver);

        logService.log("CRIADO", "ONIBUS", null,
                "Ônibus cadastrado - placa " + dto.sign() + " (" + dto.brand() + " " + dto.model() + ")");
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

        email = email.toLowerCase().trim();

        if (userRepository.findByEmail(email) != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");

        UserEntity user = new UserEntity(fullName, adress, city, email, passwordEncoder.encode(password), null, "");
        user.setRole(Role.DRIVER);
        userRepository.save(user);

        DriverEntity driver = new DriverEntity();
        driver.setUser(user);
        driver.setLicence(licence);
        driverRepository.save(driver);

        logService.log("CRIADO", "MOTORISTA", user.getId(),
                "Motorista criado: " + fullName + " (" + email + ")");
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

        int emDayCount = 0, pendingCount = 0;
        List<Map<String, Object>> passengerList = new ArrayList<>();
        for (UserEntity p : passengers) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", p.getId());
            pm.put("fullName", p.getFullName());
            pm.put("role", p.getRole().name());
            boolean isAdminOrDriver = p.getRole() == Role.ADMIN || p.getRole() == Role.DRIVER;
            pm.put("paymentStatus", !isAdminOrDriver && p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null);

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

            if (p.getRole() != Role.ADMIN && p.getRole() != Role.DRIVER) {
                if (p.getPaymentStatus() == PaymentStatus.EM_DAY) emDayCount++;
                else if (p.getPaymentStatus() == PaymentStatus.PENDING) pendingCount++;
            }

            passengerList.add(pm);
        }
        response.put("passengers", passengerList);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPassengers", passengers.size());
        stats.put("emDayCount", emDayCount);
        stats.put("pendingCount", pendingCount);
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

            if (!statusStr.equals("EM_DAY") && !statusStr.equals("PENDING"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de pagamento inválido");

            user.setPaymentStatus(PaymentStatus.valueOf(statusStr));
            userRepository.save(user);

            logService.log("ATUALIZADO", "PAGAMENTO", user.getId(),
                    "Pagamento de " + user.getFullName() + " alterado para " + statusStr);
        }
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole().name());
            m.put("adress", u.getAdress());
            m.put("city", u.getCity());
            boolean isAdminOrDriver = u.getRole() == Role.ADMIN || u.getRole() == Role.DRIVER;
            m.put("paymentStatus", !isAdminOrDriver && u.getPaymentStatus() != null ? u.getPaymentStatus().name() : null);
            m.put("userImageUrl", u.getUserImageUrl());

            int routeCount = 0;
            for (RoutesEntity r : routesRepository.findAll()) {
                if (r.getPassengers().stream().anyMatch(p -> p.getId().equals(u.getId()))) {
                    routeCount++;
                }
            }
            m.put("routeCount", routeCount);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchUsers(String query) {
        String lower = query.toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> u.getFullName().toLowerCase().contains(lower)
                        || u.getEmail().toLowerCase().contains(lower))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole().name());
                    m.put("adress", u.getAdress());
                    m.put("city", u.getCity());
                    boolean isAdminOrDriver = u.getRole() == Role.ADMIN || u.getRole() == Role.DRIVER;
                    m.put("paymentStatus", !isAdminOrDriver && u.getPaymentStatus() != null ? u.getPaymentStatus().name() : null);
                    m.put("userImageUrl", u.getUserImageUrl());
                    return m;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getUserDetails(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
        response.put("adress", user.getAdress());
        response.put("city", user.getCity());
        response.put("birthDate", user.getBirthDate() != null ? user.getBirthDate().toString() : null);
        boolean isAdminOrDriver = user.getRole() == Role.ADMIN || user.getRole() == Role.DRIVER;
        response.put("paymentStatus", !isAdminOrDriver && user.getPaymentStatus() != null ? user.getPaymentStatus().name() : null);
        response.put("userImageUrl", user.getUserImageUrl());

        List<Map<String, Object>> routes = new ArrayList<>();
        for (RoutesEntity r : routesRepository.findAll()) {
            if (r.getPassengers().stream().anyMatch(p -> p.getId().equals(user.getId()))) {
                Map<String, Object> rm = new HashMap<>();
                rm.put("idRoute", r.getIdRoute());
                rm.put("destiny", r.getDestiny());
                rm.put("departurePoint", r.getDeparturePoint());
                rm.put("code", r.getCode());
                rm.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
                rm.put("busSign", r.getBus() != null ? r.getBus().getSign() : null);
                routes.add(rm);
            }
        }
        response.put("routes", routes);

        return response;
    }

    public void updateUser(Long userId, Map<String, Object> body) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (body.containsKey("fullName")) {
            String name = (String) body.get("fullName");
            if (name == null || name.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo é obrigatório");
            user.setFullName(name.trim());
        }
        if (body.containsKey("adress")) {
            String adress = (String) body.get("adress");
            if (adress == null || adress.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço é obrigatório");
            user.setAdress(adress.trim());
        }
        if (body.containsKey("city")) {
            String city = (String) body.get("city");
            if (city == null || city.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cidade é obrigatória");
            user.setCity(city.trim());
        }
        if (body.containsKey("paymentStatus")) {
            String ps = (String) body.get("paymentStatus");
            if (ps == null || (!ps.equals("EM_DAY") && !ps.equals("PENDING")))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de pagamento inválido");
            user.setPaymentStatus(PaymentStatus.valueOf(ps));
        }

        userRepository.save(user);

        logService.log("ATUALIZOU", "USUARIO", userId,
                "Admin atualizou dados de " + user.getFullName());
    }

    public void deleteDriver(Long driverId) {
        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));

        UserEntity user = driver.getUser();

        List<BusEntity> driverBuses = busRepository.findByDriver(driver);
        for (BusEntity bus : driverBuses) {
            bus.setDriver(null);
            busRepository.save(bus);
        }

        driverRepository.delete(driver);

        if (user != null) {
            List<RoutesEntity> allRoutes = routesRepository.findAll();
            for (RoutesEntity route : allRoutes) {
                route.getPassengers().removeIf(p -> p.getId().equals(user.getId()));
                routesRepository.save(route);
            }
            List<BusEntity> allBuses = busRepository.findAll();
            for (BusEntity bus : allBuses) {
                bus.getPassengers().removeIf(p -> p.getId().equals(user.getId()));
                busRepository.save(bus);
            }
            userRepository.delete(user);
        }

        logService.log("EXCLUIU", "MOTORISTA", driverId,
                "Admin excluiu motorista " + (user != null ? user.getFullName() : "desconhecido"));
    }

    public void updateDriver(Long driverId, Map<String, Object> body) {
        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));

        UserEntity user = driver.getUser();

        if (body.containsKey("fullName")) {
            String name = (String) body.get("fullName");
            if (name == null || name.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo é obrigatório");
            user.setFullName(name.trim());
        }
        if (body.containsKey("adress")) {
            String adress = (String) body.get("adress");
            if (adress == null || adress.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço é obrigatório");
            user.setAdress(adress.trim());
        }
        if (body.containsKey("city")) {
            String city = (String) body.get("city");
            if (city == null || city.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cidade é obrigatória");
            user.setCity(city.trim());
        }
        if (body.containsKey("email")) {
            String email = ((String) body.get("email")).toLowerCase().trim();
            if (email.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
            UserEntity existing = userRepository.findByEmail(email);
            if (existing != null && !existing.getId().equals(user.getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
            user.setEmail(email);
        }
        if (body.containsKey("licence")) {
            String licence = (String) body.get("licence");
            if (licence == null || licence.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNH é obrigatória");
            driver.setLicence(licence.trim());
        }

        userRepository.save(user);
        driverRepository.save(driver);

        logService.log("ATUALIZOU", "MOTORISTA", driverId,
                "Admin atualizou dados do motorista " + user.getFullName());
    }

    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível excluir um administrador");

        List<RoutesEntity> allRoutes = routesRepository.findAll();
        for (RoutesEntity route : allRoutes) {
            route.getPassengers().removeIf(p -> p.getId().equals(userId));
            routesRepository.save(route);
        }

        List<BusEntity> allBuses = busRepository.findAll();
        for (BusEntity bus : allBuses) {
            bus.getPassengers().removeIf(p -> p.getId().equals(userId));
            busRepository.save(bus);
        }

        if (user.getRole() == Role.DRIVER) {
            DriverEntity driver = driverRepository.findByUser(user).orElse(null);
            if (driver != null) {
                List<BusEntity> driverBuses = busRepository.findByDriver(driver);
                for (BusEntity bus : driverBuses) {
                    bus.setDriver(null);
                    busRepository.save(bus);
                }
                driverRepository.delete(driver);
            }
        }

        String userName = user.getFullName();
        userRepository.delete(user);

        logService.log("EXCLUIU", "USUARIO", userId,
                "Admin excluiu usuário " + userName);
    }

    public void updateBus(Long busId, Map<String, Object> body) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        if (body.containsKey("brand")) {
            String brand = (String) body.get("brand");
            if (brand == null || brand.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marca é obrigatória");
            bus.setBrand(brand.trim());
        }
        if (body.containsKey("model")) {
            String model = (String) body.get("model");
            if (model == null || model.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modelo é obrigatório");
            bus.setModel(model.trim());
        }
        if (body.containsKey("color")) {
            String color = (String) body.get("color");
            if (color == null || color.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cor é obrigatória");
            bus.setColor(color.trim());
        }
        if (body.containsKey("sign")) {
            String sign = ((String) body.get("sign")).toUpperCase().trim();
            if (sign.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa é obrigatória");
            BusEntity existing = busRepository.findBySign(sign);
            if (existing != null && existing.getIdBus() != busId)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa já cadastrada");
            bus.setSign(sign);
        }
        if (body.containsKey("mileage")) {
            Object mileageObj = body.get("mileage");
            double mileage;
            if (mileageObj instanceof Number) {
                mileage = ((Number) mileageObj).doubleValue();
            } else {
                mileage = Double.parseDouble(mileageObj.toString());
            }
            bus.setMileage(mileage);
        }
        if (body.containsKey("busImageUrl")) {
            String imageUrl = (String) body.get("busImageUrl");
            bus.setBusImageUrl(imageUrl);
        }
        if (body.containsKey("driverId")) {
            Object driverIdObj = body.get("driverId");
            if (driverIdObj != null) {
                Long driverId = Long.valueOf(driverIdObj.toString());
                DriverEntity driver = driverRepository.findById(driverId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado"));
                bus.setDriver(driver);
            } else {
                bus.setDriver(null);
            }
        }

        busRepository.save(bus);

        logService.log("ATUALIZOU", "ONIBUS", busId,
                "Admin atualizou ônibus placa " + bus.getSign());
    }

    public void deleteBus(Long busId) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        List<RoutesEntity> routesWithBus = routesRepository.findByBus(bus);
        if (!routesWithBus.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ônibus está associado a " + routesWithBus.size() + " rota(s). Remova-o das rotas primeiro.");
        }

        String busSign = bus.getSign();
        busRepository.delete(bus);

        logService.log("EXCLUIU", "ONIBUS", busId,
                "Admin excluiu ônibus placa " + busSign);
    }

    public void updateUserPassword(Long userId, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (newPassword == null || newPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória");

        if (newPassword.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no mínimo 6 caracteres");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logService.log("ATUALIZOU", "SENHA", userId,
                "Admin alterou a senha de " + user.getFullName());
    }

    public void removeUserFromRoute(Long userId, Long routeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        boolean removedFromRoute = route.getPassengers().removeIf(p -> p.getId().equals(user.getId()));
        boolean removedFromBus = false;

        BusEntity bus = route.getBus();
        if (bus != null) {
            removedFromBus = bus.getPassengers().removeIf(p -> p.getId().equals(user.getId()));
        }

        if (!removedFromRoute && !removedFromBus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário não está inscrito nesta rota");
        }

        if (removedFromBus && bus != null) {
            busRepository.save(bus);
        }
        routesRepository.save(route);

        logService.log("REMOVIDO", "ROTA", routeId,
                user.getFullName() + " removido da rota " + route.getDeparturePoint() + " → " + route.getDestiny());
    }
}
