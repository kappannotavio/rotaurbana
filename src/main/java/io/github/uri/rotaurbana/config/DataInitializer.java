package io.github.uri.rotaurbana.config;

import io.github.uri.rotaurbana.entity.DriverEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.enums.Role;
import io.github.uri.rotaurbana.repository.DriverRepository;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepository, DriverRepository driverRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("Administrador", "Rua Admin, 123", "São Paulo",
                "admin@rotaurbana.com", "admin123", Role.ADMIN);

        UserEntity driverUser = createUserIfNotExists("Motorista Padrão", "Rua do Motorista, 456", "São Paulo",
                "driver@rotaurbana.com", "driver123", Role.DRIVER);

        if (driverUser != null && driverRepository.findByUser(driverUser).isEmpty()) {
            DriverEntity driver = new DriverEntity();
            driver.setUser(driverUser);
            driver.setLicence("12345678900");
            driverRepository.save(driver);
        }

        createUserIfNotExists("Usuário Padrão", "Rua do Passageiro, 789", "São Paulo",
                "user@rotaurbana.com", "user123", Role.USER);
    }

    private UserEntity createUserIfNotExists(String fullName, String adress, String city,
                                              String email, String password, Role role) {
        if (userRepository.findByEmail(email) != null) {
            return null;
        }
        UserEntity user = new UserEntity(fullName, adress, city, email,
                encoder.encode(password), LocalDate.of(1990, 1, 1), "padrao");
        user.setRole(role);
        return userRepository.save(user);
    }
}
