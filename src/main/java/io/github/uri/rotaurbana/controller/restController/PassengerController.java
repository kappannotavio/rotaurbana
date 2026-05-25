package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.service.passenger.PassengerService;
import io.github.uri.rotaurbana.service.tracking.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/{userId}")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private TrackingService trackingService;

    @GetMapping("/tracking/{busId}")
    public ResponseEntity<LocationRequestDTO> getLocation(Authentication auth, @PathVariable Long userId, @PathVariable Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);
        passengerService.getBusForPassenger(user, busId);

        LocationRequestDTO location = trackingService.getBusLocation(busId);
        if (location == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(location);
    }

    @GetMapping("/tracking-by-route/{routeId}")
    public ResponseEntity<LocationRequestDTO> getLocationByRoute(Authentication auth, @PathVariable Long userId, @PathVariable Long routeId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);
        passengerService.getRouteForPassenger(user, routeId);

        LocationRequestDTO location = trackingService.getRouteLocation(routeId);
        if (location == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(location);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.subscribeByBus(user, body.get("sign")));
    }

    @PostMapping("/subscribe-by-code")
    public ResponseEntity<Map<String, Object>> subscribeByCode(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.subscribeByCode(user, body.get("code")));
    }

    @PostMapping("/subscribe-by-route")
    public ResponseEntity<Map<String, Object>> subscribeByRoute(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.subscribeByRoute(user, body.get("code")));
    }

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getSubscribedBuses(Authentication auth, @PathVariable Long userId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.getSubscribedBuses(user.getId()));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> getRoutes(Authentication auth, @PathVariable Long userId, @RequestParam Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.getRoutes(busId));
    }

    @GetMapping("/all-routes")
    public ResponseEntity<List<Map<String, Object>>> getAllRoutes(Authentication auth, @PathVariable Long userId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        return ResponseEntity.ok(passengerService.getAllRoutes(user.getId()));
    }

    @PostMapping("/confirm-presence")
    public ResponseEntity<Map<String, Object>> confirmPresence(Authentication auth, @PathVariable Long userId, @RequestBody Map<String, Object> body) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        Object routeIdObj = body.get("routeId");
        String presenceType = (String) body.get("presenceType");

        Long routeId;
        if (routeIdObj instanceof Number) {
            routeId = ((Number) routeIdObj).longValue();
        } else {
            routeId = Long.parseLong(routeIdObj.toString());
        }

        return ResponseEntity.ok(passengerService.confirmPresence(user, routeId, presenceType));
    }

    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> unsubscribeRoute(Authentication auth, @PathVariable Long userId, @PathVariable Long routeId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerService.validateUser(user, userId);

        passengerService.unsubscribeRoute(user, routeId);
        return ResponseEntity.ok(Map.of("message", "Inscrição cancelada com sucesso"));
    }
}
