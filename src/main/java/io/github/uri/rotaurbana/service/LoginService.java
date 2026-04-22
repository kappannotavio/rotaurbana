package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.config.TokenService;
import io.github.uri.rotaurbana.dto.request.RegisterDTO;
import io.github.uri.rotaurbana.dto.response.AuthDTO;
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

    public String login(AuthDTO authDTO) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(authDTO.email(), authDTO.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        return tokenService.generateToken((UserEntity) auth.getPrincipal());

    }

    public boolean register(RegisterDTO registerDTO) {
        if (this.userRepository.findByEmail(registerDTO.email()) != null) return false;

        String encryptedPassword = new BCryptPasswordEncoder().encode(registerDTO.password());
        UserEntity user = new UserEntity(registerDTO.fullName(), registerDTO.adress(), registerDTO.city(), registerDTO.email(), encryptedPassword, registerDTO.birthDate());

        this.userRepository.save(user);

        return true;
    }

}
