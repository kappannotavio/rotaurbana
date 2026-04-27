package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationRequestDTO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/map")
public class MapRestController {

    Map<String, LocationRequestDTO> busLocations = new HashMap<>();

    //Recebe do Front
    @PostMapping("/{busId}")
    public void receiveLocation(@PathVariable String busId, @RequestBody LocationRequestDTO dto) {
        busLocations.put(busId, dto);
    }

    //Manda pro Front
    @GetMapping("/{busId}")
    public LocationRequestDTO getLocation(@PathVariable String busId) {
        return busLocations.get(busId);
    }

}
