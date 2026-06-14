package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.BusRequestDTO;
import io.github.uri.rotaurbana.entity.LogEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.service.LogService;
import io.github.uri.rotaurbana.service.admin.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/{adminId}")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private LogService logService;

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> listBuses(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.listBuses());
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> listRoutes(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.listRoutes());
    }

    @PostMapping("/routes")
    public ResponseEntity<Map<String, Object>> createRoute(Authentication auth, @PathVariable Long adminId, @RequestBody Map<String, String> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        var route = adminService.createRoute(body);
        return ResponseEntity.ok(Map.of("message", "Rota cadastrada com sucesso", "code", route.getCode()));
    }

    @GetMapping("/drivers")
    public ResponseEntity<List<Map<String, Object>>> listDrivers(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.listDrivers());
    }

    @PostMapping("/buses")
    public ResponseEntity<Map<String, Object>> registerBus(Authentication auth, @PathVariable Long adminId, @RequestBody BusRequestDTO dto) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.registerBus(dto);
        return ResponseEntity.ok(Map.of("message", "Ônibus cadastrado com sucesso"));
    }

    @PostMapping("/drivers")
    public ResponseEntity<Map<String, Object>> createDriver(Authentication auth, @PathVariable Long adminId, @RequestBody Map<String, String> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.createDriver(body);
        return ResponseEntity.ok(Map.of("message", "Motorista cadastrado com sucesso"));
    }

    @GetMapping("/routes/{routeId}/details")
    public ResponseEntity<Map<String, Object>> getRouteDetails(Authentication auth, @PathVariable Long adminId, @PathVariable Long routeId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.getRouteDetails(routeId));
    }

    @PutMapping("/routes/{routeId}/payment")
    public ResponseEntity<Map<String, Object>> updatePayments(Authentication auth, @PathVariable Long adminId, @PathVariable Long routeId, @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> payments = (List<Map<String, Object>>) body.get("payments");
        adminService.updatePayments(routeId, payments);

        return ResponseEntity.ok(Map.of("message", "Pagamentos atualizados com sucesso"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication auth, @PathVariable Long adminId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.listUsers());
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(Authentication auth, @PathVariable Long adminId,
                                                                  @RequestParam String q) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.searchUsers(q));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(Authentication auth, @PathVariable Long adminId,
                                                              @PathVariable Long userId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        return ResponseEntity.ok(adminService.getUserDetails(userId));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(Authentication auth, @PathVariable Long adminId,
                                                           @PathVariable Long userId,
                                                           @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.updateUser(userId, body);
        return ResponseEntity.ok(Map.of("message", "Usuário atualizado com sucesso"));
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<Map<String, Object>> updateUserPassword(Authentication auth, @PathVariable Long adminId,
                                                                   @PathVariable Long userId,
                                                                   @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        String newPassword = (String) body.get("password");
        adminService.updateUserPassword(userId, newPassword);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(Authentication auth, @PathVariable Long adminId,
                                                          @PathVariable Long userId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "Usuário excluído com sucesso"));
    }

    @PutMapping("/drivers/{driverId}")
    public ResponseEntity<Map<String, Object>> updateDriver(Authentication auth, @PathVariable Long adminId,
                                                            @PathVariable Long driverId,
                                                            @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.updateDriver(driverId, body);
        return ResponseEntity.ok(Map.of("message", "Motorista atualizado com sucesso"));
    }

    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<Map<String, Object>> deleteDriver(Authentication auth, @PathVariable Long adminId,
                                                            @PathVariable Long driverId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.deleteDriver(driverId);
        return ResponseEntity.ok(Map.of("message", "Motorista excluído com sucesso"));
    }

    @PutMapping("/buses/{busId}")
    public ResponseEntity<Map<String, Object>> updateBus(Authentication auth, @PathVariable Long adminId,
                                                          @PathVariable Long busId,
                                                          @RequestBody Map<String, Object> body) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.updateBus(busId, body);
        return ResponseEntity.ok(Map.of("message", "Ônibus atualizado com sucesso"));
    }

    @DeleteMapping("/buses/{busId}")
    public ResponseEntity<Map<String, Object>> deleteBus(Authentication auth, @PathVariable Long adminId,
                                                          @PathVariable Long busId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.deleteBus(busId);
        return ResponseEntity.ok(Map.of("message", "Ônibus excluído com sucesso"));
    }

    @DeleteMapping("/users/{userId}/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> removeUserFromRoute(Authentication auth, @PathVariable Long adminId,
                                                                   @PathVariable Long userId,
                                                                   @PathVariable Long routeId) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        adminService.removeUserFromRoute(userId, routeId);
        return ResponseEntity.ok(Map.of("message", "Usuário removido da rota com sucesso"));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntity>> listLogs(Authentication auth, @PathVariable Long adminId,
                                                    @RequestParam(required = false) String type) {
        UserEntity admin = (UserEntity) auth.getPrincipal();
        adminService.validateAdmin(admin, adminId);
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(logService.getLogsByEntityType(type.toUpperCase()));
        }
        return ResponseEntity.ok(logService.getAllLogs());
    }
}
