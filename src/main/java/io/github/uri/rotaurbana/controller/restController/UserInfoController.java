package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.response.UserResponseDTO;
import io.github.uri.rotaurbana.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/user")
public class UserInfoController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> userDetails(@PathVariable Long id) {

        UserResponseDTO user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String adress,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate birthDate,
            @RequestParam(required = false) String userImageUrl) {

        UserResponseDTO updated = userService.updateUser(id, fullName, adress, city, birthDate, userImageUrl);

        return ResponseEntity.ok(updated);
    }
}
