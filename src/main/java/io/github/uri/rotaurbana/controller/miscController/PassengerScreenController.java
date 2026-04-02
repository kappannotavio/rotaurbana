package io.github.uri.rotaurbana.controller.miscController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PassengerScreenController {
    
    @GetMapping("/passenger")
    public String passengerScreen() {
        return "PassengerScreen";
    }
}
