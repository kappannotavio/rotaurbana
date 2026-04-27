package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.response.UserResponseDTO;
import io.github.uri.rotaurbana.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
