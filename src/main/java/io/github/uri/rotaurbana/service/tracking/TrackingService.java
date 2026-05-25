package io.github.uri.rotaurbana.service.tracking;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {

    private final Map<Long, LocationDTO> busLocations = new ConcurrentHashMap<>();
    private final Map<Long, LocationDTO> routeLocations = new ConcurrentHashMap<>();

    private static final long LOCATION_TTL_MS = 5000;

    public void updateBusLocation(Long busId, LocationRequestDTO dto) {
        busLocations.put(busId, new LocationDTO(dto.latitude(), dto.longitude(), System.currentTimeMillis()));
    }

    public void updateRouteLocation(Long routeId, LocationRequestDTO dto) {
        routeLocations.put(routeId, new LocationDTO(dto.latitude(), dto.longitude(), System.currentTimeMillis()));
    }

    public LocationRequestDTO getBusLocation(Long busId) {
        LocationDTO loc = busLocations.get(busId);
        if (loc == null) return null;
        if (System.currentTimeMillis() - loc.timestamp > LOCATION_TTL_MS) {
            busLocations.remove(busId);
            return null;
        }
        return new LocationRequestDTO(loc.latitude, loc.longitude);
    }

    public LocationRequestDTO getRouteLocation(Long routeId) {
        LocationDTO loc = routeLocations.get(routeId);
        if (loc == null) return null;
        if (System.currentTimeMillis() - loc.timestamp > LOCATION_TTL_MS) {
            routeLocations.remove(routeId);
            return null;
        }
        return new LocationRequestDTO(loc.latitude, loc.longitude);
    }

    private record LocationDTO(double latitude, double longitude, long timestamp) {}
}
