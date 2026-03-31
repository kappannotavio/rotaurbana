package io.github.uri.rotaurbana.controller.miscController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapController {

    @GetMapping("/map")
    public String mapRender(){
        return "map";
    }

}
