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

import java.time.LocalDate;
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

    @Autowired
    private LogService logService;

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

    public void register(RegisterRequestDTO dto) {
        String fullName = dto.fullName();
        String adress = dto.adress();
        String city = dto.city();
        String email = dto.email();
        String password = dto.password();
        String confirmPassword = dto.confirmPassword();
        LocalDate birthDate = dto.birthDate();

        if (fullName == null || fullName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome completo é obrigatório");

        if (adress == null || adress.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço é obrigatório");

        if (city == null || city.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cidade é obrigatória");

        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");

        email = email.toLowerCase().trim();

        if (!email.matches("^[\\w-.]+@[\\w-]+\\.[\\w.-]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido");

        if (password == null || password.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória");

        if (password.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no mínimo 6 caracteres");

        if (confirmPassword == null || confirmPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmação de senha é obrigatória");

        if (!password.equals(confirmPassword))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senhas não conferem");

        if (birthDate == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de nascimento é obrigatória");

        if (birthDate.isAfter(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de nascimento deve ser no passado");

        if (this.userRepository.findByEmail(email) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");

        String encryptedPassword = passwordEncoder.encode(password);
        UserEntity user = new UserEntity(fullName, adress, city, email, encryptedPassword, birthDate,
                dto.userImageUrl());

        this.userRepository.save(user);

        logService.log("CRIADO", "USUARIO", user.getId(),
                "Novo usuário: " + user.getFullName() + " (" + email + ")");
    }

}
