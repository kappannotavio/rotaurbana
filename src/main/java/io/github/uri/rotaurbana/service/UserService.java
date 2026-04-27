package io.github.uri.rotaurbana.service;

import io.github.uri.rotaurbana.dto.response.UserResponseDTO;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public UserResponseDTO getUserById(Long id) {

        UserEntity loggedUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!loggedUser.getId().equals(id) && !loggedUser.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Access denied!");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        return new UserResponseDTO(
                user.getFullName(),
                user.getAdress(),
                user.getCity(),
                user.getRole(),
                user.getEmail(),
                user.getBirthDate(),
                user.getUserImageUrl()
        );
    }

}
