package io.github.uri.rotaurbana.controller.miscController;

import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DriverPageController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/driver")
    public String driverPage(Authentication auth, Model model) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        UserEntity freshUser = userRepository.findById(user.getId()).orElse(user);
        model.addAttribute("userName", freshUser.getFullName());
        model.addAttribute("userId", freshUser.getId());
        model.addAttribute("userImageUrl", freshUser.getUserImageUrl());
        return "driver/driver";
    }

}
