package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.rang.DokazUslovaDTO;
import ba.unze.edom.common.dto.rang.ObjavaRangListeDTO;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangObjavaDTO;
import ba.unze.edom.server.service.rang.DokazUslovaService;
import ba.unze.edom.server.service.rang.RangListaObjavaService;
import ba.unze.edom.server.service.rang.RangListaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rang-lista")
public class RangListaApiController {

    private final RangListaService rangListaService;
    private final DokazUslovaService dokazUslovaService;
    private final RangListaObjavaService objavaService;

    public RangListaApiController(RangListaService rangListaService,
                                  DokazUslovaService dokazUslovaService,
                                  RangListaObjavaService objavaService) {
        this.rangListaService = rangListaService;
        this.dokazUslovaService = dokazUslovaService;
        this.objavaService = objavaService;
    }

    /** Lista uživo (radni prostor admina). */
    @GetMapping
    public RangListaAdminDTO dohvati(@RequestParam(required = false) Integer godina,
                                     @RequestParam(required = false) Integer kapacitet,
                                     @RequestParam(defaultValue = "false") boolean popuniBrucosima) {
        return rangListaService.zaAdmina(godina, kapacitet, popuniBrucosima);
    }

    /** "Dokaz ✓": na čekanju → odobreno. */
    @PostMapping("/prijave/{id}/dokaz-uslova")
    public DokazUslovaDTO evidentirajDokaz(@PathVariable Integer id) {
        return dokazUslovaService.evidentiraj(id);
    }

    /** "Poništi dokaz": odobreno → na čekanju. */
    @PostMapping("/prijave/{id}/dokaz-uslova/ponisti")
    public DokazUslovaDTO ponistiDokaz(@PathVariable Integer id) {
        return dokazUslovaService.ponisti(id);
    }

    /** "Objavi na webu": snimi trenutnu listu; web je prikazuje dok se ne objavi nova. */
    @PostMapping("/objavi")
    public RangObjavaDTO objavi(@RequestBody ObjavaRangListeDTO z) {
        return objavaService.objavi(z);
    }

    /** Šta je trenutno objavljeno na webu. */
    @GetMapping("/objava")
    public RangObjavaDTO zadnjaObjava() {
        return objavaService.zadnjaInfo();
    }
}