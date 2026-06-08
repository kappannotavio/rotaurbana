package io.github.uri.rotaurbana.service.passenger;

import io.github.uri.rotaurbana.entity.*;
import io.github.uri.rotaurbana.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PassengerService {

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Transactional
    public Map<String, Object> subscribeByBus(UserEntity user, String sign) {
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
        return response;
    }

    @Transactional
    public Map<String, Object> subscribeByCode(UserEntity user, String code) {
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
        return response;
    }

    @Transactional
    public Map<String, Object> subscribeByRoute(UserEntity user, String code) {
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
        return response;
    }

    public List<Map<String, Object>> getSubscribedBuses(Long userId) {
        List<BusEntity> buses = busRepository.findByPassengers_Id(userId);

        return buses.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idBus", b.getIdBus());
            m.put("sign", b.getSign());
            m.put("model", b.getModel());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRoutes(Long busId) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        return routesRepository.findByBus(bus).stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idRoute", r.getIdRoute());
            m.put("destiny", r.getDestiny());
            m.put("departureTime", r.getDepartureTime() != null ? r.getDepartureTime().toString() : null);
            m.put("destinationTime", r.getDestinationTime() != null ? r.getDestinationTime().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllRoutes(Long userId) {
        List<RoutesEntity> routes = routesRepository.findByPassengers_Id(userId);
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
        return result;
    }

    @Transactional
    public Map<String, Object> confirmPresence(UserEntity user, Long routeId, String presenceType) {
        if (routeId == null || presenceType == null || presenceType.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routeId e presenceType são obrigatórios");

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
        return response;
    }

    @Transactional
    public void unsubscribeRoute(UserEntity user, Long routeId) {
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
    }

    public void validateUser(UserEntity user, Long userId) {
        if (!user.getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    public BusEntity getBusForPassenger(UserEntity user, Long busId) {
        BusEntity bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ônibus não encontrado"));

        boolean isPassenger = bus.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!isPassenger)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este ônibus");

        return bus;
    }

    public RoutesEntity getRouteForPassenger(UserEntity user, Long routeId) {
        RoutesEntity route = routesRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota não encontrada"));

        boolean isSubscribed = route.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!isSubscribed)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a esta rota");

        return route;
    }
}
