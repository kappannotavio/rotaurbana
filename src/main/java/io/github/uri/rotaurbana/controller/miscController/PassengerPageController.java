package io.github.uri.rotaurbana.controller.miscController;

import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import io.github.uri.rotaurbana.repository.UserRepository;
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
    private UserRepository userRepository;

    @Autowired
    private RoutesRepository routesRepository;

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        UserEntity user = (UserEntity) auth.getPrincipal();

        UserEntity freshUser = userRepository.findById(user.getId()).orElse(user);

        model.addAttribute("userName", freshUser.getFullName());
        model.addAttribute("userId", freshUser.getId());
        model.addAttribute("userImageUrl", freshUser.getUserImageUrl());

        String paymentLabel;
        if (freshUser.getPaymentStatus() == null) {
            paymentLabel = "Pagamento em dia";
        } else {
            paymentLabel = switch (freshUser.getPaymentStatus()) {
                case EM_DAY -> "Pagamento em dia";
                case PENDING -> "Pagamento Pendente";
                case LATE -> "Pagamento Atrasado";
            };
        }
        model.addAttribute("paymentStatus", paymentLabel);

        return "passenger";
    }

    @GetMapping("/map")
    public String map(@RequestParam(required = false) Long routeId, Model model) {
        String routeTitle = "Rota";
        String routeSubtitle = null;
        String departurePoint = null;
        String destiny = null;
        String departureTime = null;
        String estimatedDuration = null;
        Double departureLatitude = null;
        Double departureLongitude = null;
        Double destinationLatitude = null;
        Double destinationLongitude = null;

        if (routeId != null) {
            RoutesEntity route = routesRepository.findById(routeId).orElse(null);
            if (route != null) {
                String origem = route.getDeparturePoint() != null ? route.getDeparturePoint() : "Origem";
                routeTitle = origem + " \u2192 " + route.getDestiny();
                routeSubtitle = "Rota ao vivo";
                departurePoint = route.getDeparturePoint();
                destiny = route.getDestiny();
                departureTime = route.getDepartureTime() != null ? route.getDepartureTime().toString() : null;
                estimatedDuration = route.getEstimatedDuration();
                departureLatitude = route.getDepartureLatitude();
                departureLongitude = route.getDepartureLongitude();
                destinationLatitude = route.getDestinationLatitude();
                destinationLongitude = route.getDestinationLongitude();
            }
        }

        model.addAttribute("routeTitle", routeTitle);
        model.addAttribute("routeSubtitle", routeSubtitle);
        model.addAttribute("departurePoint", departurePoint);
        model.addAttribute("destiny", destiny);
        model.addAttribute("departureTime", departureTime);
        model.addAttribute("estimatedDuration", estimatedDuration);
        model.addAttribute("departureLatitude", departureLatitude);
        model.addAttribute("departureLongitude", departureLongitude);
        model.addAttribute("destinationLatitude", destinationLatitude);
        model.addAttribute("destinationLongitude", destinationLongitude);
        return "passenger-map";
    }

}
