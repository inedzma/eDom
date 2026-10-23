package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.PorukaDTO;
import ba.unze.edom.server.service.PorukeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/poruke")
@RequiredArgsConstructor
public class PorukeApiController {

    private final PorukeService porukeService;

    @GetMapping
    public List<PorukaDTO> nepročitane() {
        return porukeService.zaAdmina();
    }

    @PostMapping("/{id}/procitano")
    public Map<String, Boolean> procitano(@PathVariable String id) {
        porukeService.procitano(id);
        return Map.of("ok", true);
    }
}