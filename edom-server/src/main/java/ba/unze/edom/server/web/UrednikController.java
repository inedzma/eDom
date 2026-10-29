package ba.unze.edom.server.web;

import ba.unze.edom.server.service.GalerijaService;
import ba.unze.edom.server.service.WebTekstoviService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Uređivanje javnih stranica (samo admin; zaštita u SecurityConfig). */
@RestController
@RequestMapping("/urednik")
@RequiredArgsConstructor
public class UrednikController {

    private final WebTekstoviService webTekstovi;
    private final GalerijaService galerija;

    @PostMapping("/tekstovi")
    public Map<String, Object> sacuvaj(@RequestBody Map<String, String> izmjene) {
        webTekstovi.sacuvaj(izmjene);
        return Map.of("ok", true, "sacuvano", izmjene.size());
    }

    @PostMapping("/galerija")
    public Map<String, Object> dodajSlike(@RequestParam("slike") List<MultipartFile> slike) {
        return Map.of("ok", true, "dodano", galerija.dodaj(slike));
    }

    @PostMapping("/galerija/{ime}/obrisi")
    public Map<String, Object> obrisiSliku(@PathVariable String ime) {
        galerija.obrisi(ime);
        return Map.of("ok", true);
    }

    /** Poruka greške ide u JSON, da je stranica može prikazati adminu. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> greska(ResponseStatusException e) {
        String poruka = e.getReason() != null ? e.getReason() : "Greška na serveru.";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("ok", false, "message", poruka));
    }
}