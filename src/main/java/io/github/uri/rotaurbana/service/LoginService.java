package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.config.TokenService;
import io.github.uri.rotaurbana.dto.request.RegisterRequestDTO;
import io.github.uri.rotaurbana.dto.response.AuthResponseDTO;
import io.github.uri.rotaurbana.dto.response.LoginResponseDTO;
import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class LoginService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(AuthResponseDTO authResponseDTO) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(authResponseDTO.email(), authResponseDTO.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        UserEntity user = (UserEntity) auth.getPrincipal();
        String token = tokenService.generateToken(user);

        Long driverId = null;
        if (user.getRole() == Role.DRIVER) {
            Optional<DriverEntity> driver = driverRepository.findByUser(user);
            if (driver.isPresent()) {
                driverId = driver.get().getIdDriver();
            }
        }

        return new LoginResponseDTO(
                token,
                user.getId(),
                driverId,
                user.getRole().name()
        );
    }

    public boolean register(RegisterRequestDTO registerRequestDTO) {
        String email = registerRequestDTO.email().toLowerCase().trim();

        if (!registerRequestDTO.password().equals(registerRequestDTO.confirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senhas não conferem");

        if (this.userRepository.findByEmail(email) != null) return false;

        String encryptedPassword = passwordEncoder.encode(registerRequestDTO.password());
        UserEntity user = new UserEntity(
                registerRequestDTO.fullName(),
                registerRequestDTO.adress(),
                registerRequestDTO.city(),
                email,
                encryptedPassword,
                registerRequestDTO.birthDate(),
                registerRequestDTO.userImageUrl());

        this.userRepository.save(user);

        return true;
    }

}
