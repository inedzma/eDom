package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.RangListaAdminDTO;
import ba.unze.edom.server.service.RangListaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rang-lista")
@RequiredArgsConstructor
public class RangListaApiController {

    private final RangListaService rangListaService;

    @GetMapping
    public RangListaAdminDTO rangLista(@RequestParam(required = false) Integer godina) {
        return rangListaService.zaAdmina(godina);
    }
}