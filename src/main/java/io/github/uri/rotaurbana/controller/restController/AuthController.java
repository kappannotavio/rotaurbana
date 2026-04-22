package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.RegisterDTO;
import io.github.uri.rotaurbana.dto.response.AuthDTO;
import io.github.uri.rotaurbana.dto.response.LoginDTO;
import io.github.uri.rotaurbana.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    LoginService loginService = new LoginService();

    @PostMapping("/login")
    public ResponseEntity<LoginDTO> login(@RequestBody @Valid AuthDTO authDTO) {

        var token = loginService.login(authDTO);

        return ResponseEntity.ok(new LoginDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO registerDTO) {

        boolean success = loginService.register(registerDTO);

        if(!success) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();

    }

}
