package io.github.uri.rotaurbana.controller.miscController;

import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.service.passenger.PassengerViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/passenger")
public class PassengerPageController {

    @Autowired
    private PassengerViewService passengerViewService;

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        UserEntity user = (UserEntity) auth.getPrincipal();
        passengerViewService.prepareDashboardModel(user, model);
        return "passenger/passenger";
    }

    @GetMapping("/map")
    public String map(@RequestParam(required = false) Long routeId, Model model) {
        passengerViewService.prepareMapModel(routeId, model);
        return "passenger/passenger-map";
    }

}
