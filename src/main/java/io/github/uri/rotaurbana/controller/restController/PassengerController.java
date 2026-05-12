package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import io.github.uri.rotaurbana.entity.BusEntity;
import io.github.uri.rotaurbana.entity.PresenceEntity;
import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.BusRepository;
import io.github.uri.rotaurbana.repository.PresenceRepository;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/passenger/{userId}")
public class PassengerController {

    private final Map<Long, LocationRequestDTO> busLocations = new HashMap<>();
    private final Map<Long, Long> busLastUpdate = new HashMap<>();
    private final Map<Long, LocationRequestDTO> routeLocations = new HashMap<>();
    private final Map<Long, Long> routeLastUpdate = new HashMap<>();
    private static final long LOCATION_TTL_MS = 5000;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @GetMapping("/tracking/{busId}")
    public ResponseEntity<LocationRequestDTO> getLocation(Authentication auth, @PathVariable Long userId, @PathVariable Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        boolean isPassenger = bus.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!isPassenger)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este ônibus");

        Long lastUpdate = busLastUpdate.get(busId);
        if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > LOCATION_TTL_MS) {
            busLocations.remove(busId);
            busLastUpdate.remove(busId);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(busLocations.get(busId));
    }

    @GetMapping("/tracking-by-route/{routeId}")
    public ResponseEntity<LocationRequestDTO> getLocationByRoute(Authentication auth, @PathVariable Long userId, @PathVariable Long routeId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        boolean isSubscribed = route.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!isSubscribed)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a esta rota");

        Long lastUpdate = routeLastUpdate.get(routeId);
        if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > LOCATION_TTL_MS) {
            routeLocations.remove(routeId);
            routeLastUpdate.remove(routeId);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(routeLocations.get(routeId));
    }

    synchronized void updateRouteLocation(Long routeId, LocationRequestDTO dto) {
        routeLocations.put(routeId, dto);
        routeLastUpdate.put(routeId, System.currentTimeMillis());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        String sign = body.get("sign");
        if (sign == null || sign.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa é obrigatória");

        BusEntity bus = busRepository.findBySign(sign);
        if (bus == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado com esta placa");

        boolean alreadySubscribed = bus.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!alreadySubscribed) {
            bus.getPassengers().add(user);
            busRepository.save(bus);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("idBus", bus.getIdBus());
        response.put("sign", bus.getSign());
        response.put("message", "Inscrito com sucesso");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe-by-code")
    public ResponseEntity<Map<String, Object>> subscribeByCode(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        String code = body.get("code");
        if (code == null || code.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código é obrigatório");

        BusEntity bus = busRepository.findByCode(code.toUpperCase());
        if (bus == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado com este código");

        boolean alreadySubscribed = bus.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!alreadySubscribed) {
            bus.getPassengers().add(user);
            busRepository.save(bus);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("idBus", bus.getIdBus());
        response.put("sign", bus.getSign());
        response.put("message", "Inscrito com sucesso");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe-by-route")
    public ResponseEntity<Map<String, Object>> subscribeByRoute(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        String code = body.get("code");
        if (code == null || code.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código é obrigatório");

        RoutesEntity route = routesRepository.findByCode(code.toUpperCase());
        if (route == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada com este código");

        boolean alreadySubscribed = route.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!alreadySubscribed) {
            route.getPassengers().add(user);
            routesRepository.save(route);
        }

        BusEntity bus = route.getBus();

        boolean alreadyOnBus = bus.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!alreadyOnBus) {
            bus.getPassengers().add(user);
            busRepository.save(bus);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("idBus", bus.getIdBus());
        response.put("sign", bus.getSign());
        response.put("idRoute", route.getIdRoute());
        response.put("destiny", route.getDestiny());
        response.put("message", "Inscrito na rota com sucesso");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getSubscribedBuses(Authentication auth, @PathVariable Long userId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<BusEntity> buses = busRepository.findByPassengers_Id(user.getId());

        List<Map<String, Object>> result = buses.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idBus", b.getIdBus());
            m.put("sign", b.getSign());
            m.put("model", b.getModel());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> getRoutes(Authentication auth, @PathVariable Long userId, @RequestParam Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        List<RoutesEntity> routes = routesRepository.findByBus(bus);

        List<Map<String, Object>> result = routes.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all-routes")
    public ResponseEntity<List<Map<String, Object>>> getAllRoutes(Authentication auth, @PathVariable Long userId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        List<RoutesEntity> routes = routesRepository.findByPassengers_Id(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (RoutesEntity r : routes) {
            BusEntity bus = r.getBus();
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            m.put("departurePoint", r.getDeparturePoint());
            m.put("departureAddress", r.getDepartureAddress());
            m.put("destinationAddress", r.getDestinationAddress());
            m.put("estimatedDuration", r.getEstimatedDuration());
            m.put("departureLatitude", r.getDepartureLatitude());
            m.put("departureLongitude", r.getDepartureLongitude());
            m.put("destinationLatitude", r.getDestinationLatitude());
            m.put("destinationLongitude", r.getDestinationLongitude());
            m.put("busModel", bus != null ? bus.getModel() : null);
            m.put("busSign", bus != null ? bus.getSign() : null);
            m.put("busId", bus != null ? bus.getIdBus() : null);
            m.put("code", r.getCode());
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/confirm-presence")
    public ResponseEntity<Map<String, Object>> confirmPresence(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        Object routeIdObj = body.get("routeId");
        String presenceType = (String) body.get("presenceType");

        if (routeIdObj == null || presenceType == null || presenceType.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routeId e presenceType são obrigatórios");

        Long routeId;
        if (routeIdObj instanceof Number) {
            routeId = ((Number) routeIdObj).longValue();
        } else {
            routeId = Long.parseLong(routeIdObj.toString());
        }

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        LocalDate today = LocalDate.now();

        Optional<PresenceEntity> existing = presenceRepository.findByUserAndRouteAndDate(user, route, today);

        PresenceEntity presence;
        if (existing.isPresent()) {
            presence = existing.get();
            presence.setPresenceType(presenceType);
        } else {
            presence = new PresenceEntity();
            presence.setUser(user);
            presence.setRoute(route);
            presence.setDate(today);
            presence.setPresenceType(presenceType);
        }

        presenceRepository.save(presence);

        Map<String, Object> response = new HashMap<>();
        response.put("routeId", routeId);
        response.put("presenceType", presenceType);
        response.put("userId", user.getId());
        response.put("message", "Presença confirmada com sucesso");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> unsubscribeRoute(Authentication auth, @PathVariable Long userId, @PathVariable Long routeId) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");

        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        boolean wasSubscribed = route.getPassengers().removeIf(p -> p.getId().equals(user.getId()));

        if (!wasSubscribed)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não está inscrito nesta rota");

        BusEntity bus = route.getBus();
        if (bus != null) {
            bus.getPassengers().removeIf(p -> p.getId().equals(user.getId()));
            busRepository.save(bus);
        }

        routesRepository.save(route);

        return ResponseEntity.ok(Map.of("message", "Inscrição cancelada com sucesso"));
    }

    synchronized void updateLocation(Long busId, LocationRequestDTO dto) {
        busLocations.put(busId, dto);
        busLastUpdate.put(busId, System.currentTimeMillis());
    }

}
