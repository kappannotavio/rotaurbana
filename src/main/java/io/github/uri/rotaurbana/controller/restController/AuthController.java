package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.RegisterRequestDTO;
import io.github.uri.rotaurbana.dto.response.AuthResponseDTO;
import io.github.uri.rotaurbana.dto.response.LoginResponseDTO;
import io.github.uri.rotaurbana.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity register(@RequestBody RegisterRequestDTO dto) {

        loginService.register(dto);

        return ResponseEntity.ok().build();

    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
    }

}
