package io.github.uri.rotaurbana.controller.miscController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminPage() {
        return "redirect:/admin/routes";
    }

    @GetMapping("/admin/route-details/{routeId}")
    public String routeDetailsPage() {
        return "route-details-admin";
    }

    @GetMapping("/admin/routes")
    public String routeListPage() {
        return "route-list-admin";
    }

    @GetMapping("/admin/vehicles")
    public String vehicleRegisterPage() {
        return "veichle-register-admin";
    }

    @GetMapping("/admin/drivers")
    public String driverRegisterPage() {
        return "driver-register-admin";
    }

}
