package io.github.uri.rotaurbana.service.passenger;

import io.github.uri.rotaurbana.entity.RoutesEntity;
import io.github.uri.rotaurbana.entity.UserEntity;
import io.github.uri.rotaurbana.repository.RoutesRepository;
import io.github.uri.rotaurbana.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.Optional;

@Service
public class PassengerViewService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoutesRepository routesRepository;

    public UserEntity getFreshUser(UserEntity user) {
        return userRepository.findById(user.getId()).orElse(user);
    }

    public void prepareDashboardModel(UserEntity user, Model model) {
        UserEntity freshUser = getFreshUser(user);

        model.addAttribute("userName", freshUser.getFullName());
        model.addAttribute("userId", freshUser.getId());
        model.addAttribute("userImageUrl", freshUser.getUserImageUrl());

        String paymentLabel;
        String paymentStatusName;
        if (freshUser.getPaymentStatus() == null) {
            paymentLabel = "Pagamento em dia";
            paymentStatusName = "EM_DAY";
        } else {
            paymentStatusName = freshUser.getPaymentStatus().name();
            paymentLabel = switch (freshUser.getPaymentStatus()) {
                case EM_DAY -> "Pagamento em dia";
                case PENDING -> "Pagamento Pendente";
            };
        }
        model.addAttribute("paymentStatus", paymentLabel);
        model.addAttribute("paymentStatusName", paymentStatusName);
    }

    public void prepareMapModel(Long routeId, Model model) {
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
            Optional<RoutesEntity> optRoute = routesRepository.findById(routeId);
            if (optRoute.isPresent()) {
                RoutesEntity route = optRoute.get();
                String origem = route.getDeparturePoint() != null ? route.getDeparturePoint() : "Origem";
                routeTitle = origem + " → " + route.getDestiny();
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
    }
}
