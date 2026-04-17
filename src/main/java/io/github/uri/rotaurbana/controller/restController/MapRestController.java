package io.github.uri.rotaurbana.controller.restController;

import io.github.uri.rotaurbana.dto.request.LocationDTO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/map")
public class MapRestController {

    Map<String, LocationDTO> busLocations = new HashMap<>();

    //Recebe do Front
    @PostMapping("/{busId}")
    public void receiveLocation(@PathVariable String busId, @RequestBody LocationDTO dto) {
        busLocations.put(busId, dto);
    }

    //Manda pro Front
    @GetMapping("/{busId}")
    public LocationDTO getLocation(@PathVariable String busId) {
        return busLocations.get(busId);
    }

}
