package io.github.uri.rotaurbana.controller.miscController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminPage() {
        return "redirect:/admin/routes";
    }

    @GetMapping("/admin/route-details/{routeId}")
    public String routeDetailsPage() {
        return "admin/route-details-admin";
    }

    @GetMapping("/admin/routes")
    public String routeListPage() {
        return "admin/route-list-admin";
    }

    @GetMapping("/admin/vehicles")
    public String vehicleRegisterPage() {
        return "admin/veichle-register-admin";
    }

    @GetMapping("/admin/drivers")
    public String driverRegisterPage() {
        return "admin/driver-register-admin";
    }

}
