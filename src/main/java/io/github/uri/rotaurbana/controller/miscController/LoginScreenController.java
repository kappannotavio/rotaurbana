package io.github.uri.rotaurbana.controller.miscController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginScreenController {

    @GetMapping("/auth/login")
    public String login(){
        return "LoginScreen";
    }

}
