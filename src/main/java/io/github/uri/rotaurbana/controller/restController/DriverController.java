package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.service.driver.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/{driverId}")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping("/tracking/{busId}")
    public void sendLocation(Authentication auth, @PathVariable Long driverId, @PathVariable Long busId, @RequestBody LocationRequestDTO dto) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        DriverEntity driver = driverService.validateAndGetDriver(user, driverId);
        driverService.sendLocation(driver, busId, dto);
    }

    @PostMapping("/routes/{routeId}/tracking")
    public void sendRouteLocation(Authentication auth, @PathVariable Long driverId, @PathVariable Long routeId, @RequestBody LocationRequestDTO dto) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        DriverEntity driver = driverService.validateAndGetDriver(user, driverId);
        driverService.sendRouteLocation(driver, routeId, dto);
    }

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getMyBuses(Authentication auth, @PathVariable Long driverId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        DriverEntity driver = driverService.validateAndGetDriver(user, driverId);
        return ResponseEntity.ok(driverService.getMyBuses(driver));
    }

    @GetMapping("/buses/{busId}/routes")
    public ResponseEntity<List<Map<String, Object>>> getBusRoutes(Authentication auth, @PathVariable Long driverId, @PathVariable Long busId) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        DriverEntity driver = driverService.validateAndGetDriver(user, driverId);
        return ResponseEntity.ok(driverService.getBusRoutes(driver, busId));
    }
}
