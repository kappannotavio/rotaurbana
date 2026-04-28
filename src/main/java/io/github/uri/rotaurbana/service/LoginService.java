package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.config.TokenService;
import io.github.uri.rotaurbana.dto.request.RegisterRequestDTO;
import io.github.uri.rotaurbana.dto.response.AuthResponseDTO;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class LoginService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    public String login(AuthResponseDTO authResponseDTO) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(authResponseDTO.email(), authResponseDTO.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        return tokenService.generateToken((UserEntity) auth.getPrincipal());

    }

    public boolean register(RegisterRequestDTO registerRequestDTO) {
        if (this.userRepository.findByEmail(registerRequestDTO.email()) != null) return false;

        String encryptedPassword = new BCryptPasswordEncoder().encode(registerRequestDTO.password());
        UserEntity user = new UserEntity(
                registerRequestDTO.fullName(),
                registerRequestDTO.adress(),
                registerRequestDTO.city(),
                registerRequestDTO.email(),
                encryptedPassword,
                registerRequestDTO.birthDate(),
                registerRequestDTO.userImageUrl());

        this.userRepository.save(user);

        return true;
    }

}
