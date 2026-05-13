package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.RegisterRequestDTO;
import io.github.uri.rotaurbana.dto.response.AuthResponseDTO;
import io.github.uri.rotaurbana.dto.response.LoginResponseDTO;
import io.github.uri.rotaurbana.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthResponseDTO authResponseDTO) {

        LoginResponseDTO response = loginService.login(authResponseDTO);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterRequestDTO registerRequestDTO) {

        boolean success = loginService.register(registerRequestDTO);

        if(!success) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();

    }

}
