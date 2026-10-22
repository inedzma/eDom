package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.DokazUslovaDTO;
import ba.unze.edom.common.dto.RangListaAdminDTO;
import ba.unze.edom.server.service.DokazUslovaService;
import ba.unze.edom.server.service.RangListaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rang-lista")
public class RangListaApiController {

    private final RangListaService rangListaService;
    private final DokazUslovaService dokazUslovaService;

    public RangListaApiController(RangListaService rangListaService, DokazUslovaService dokazUslovaService) {
        this.rangListaService = rangListaService;
        this.dokazUslovaService = dokazUslovaService;
    }

    /** Kapacitet i udio brucoša čita servis iz application.properties; parametar kapacitet ih može nadjačati. */
    @GetMapping
    public RangListaAdminDTO dohvati(@RequestParam(required = false) Integer godina,
                                     @RequestParam(required = false) Integer kapacitet,
                                     @RequestParam(defaultValue = "false") boolean popuniBrucosima) {
        return rangListaService.zaAdmina(godina, kapacitet, popuniBrucosima);
    }

    /** "Dokaz ✓": na čekanju → odobreno; admina servis uzima iz prijavljenog korisnika. */
    @PostMapping("/prijave/{id}/dokaz-uslova")
    public DokazUslovaDTO evidentirajDokaz(@PathVariable Integer id) {
        return dokazUslovaService.evidentiraj(id);
    }

    /** "Poništi dokaz": odobreno → na čekanju (samo za naknadno evidentiran dokaz). */
    @PostMapping("/prijave/{id}/dokaz-uslova/ponisti")
    public DokazUslovaDTO ponistiDokaz(@PathVariable Integer id) {
        return dokazUslovaService.ponisti(id);
    }
}