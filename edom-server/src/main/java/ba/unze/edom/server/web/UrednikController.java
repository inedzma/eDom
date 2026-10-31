package ba.unze.edom.server.web;

import ba.unze.edom.server.service.javna.GalerijaService;
import ba.unze.edom.server.service.javna.JavniDokumentiService;
import ba.unze.edom.server.service.javna.ObavjestenjaService;
import ba.unze.edom.server.service.javna.WebTekstoviService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/** Uređivanje javnih stranica (samo admin; zaštita u SecurityConfig). */
@RestController
@RequestMapping("/urednik")
@RequiredArgsConstructor
public class UrednikController {

    private final WebTekstoviService webTekstovi;
    private final GalerijaService galerija;

    private final JavniDokumentiService javniDokumenti;
    private final ObavjestenjaService obavjestenjaService;

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

    @PostMapping("/dokumenti")
    public Map<String, Object> dodajDokument(@RequestParam String naslov,
                                             @RequestParam String kategorija,
                                             @RequestParam(required = false) LocalDate datum,
                                             @RequestParam("fajl") MultipartFile fajl) {
        javniDokumenti.dodaj(naslov, kategorija, datum, fajl);
        return Map.of("ok", true);
    }

    @PostMapping("/dokumenti/{id}/izmijeni")
    public Map<String, Object> izmijeniDokument(@PathVariable String id,
                                                @RequestParam String naslov,
                                                @RequestParam String kategorija,
                                                @RequestParam(required = false) LocalDate datum) {
        javniDokumenti.izmijeni(id, naslov, kategorija, datum);
        return Map.of("ok", true);
    }

    @PostMapping("/dokumenti/{id}/obrisi")
    public Map<String, Object> obrisiDokument(@PathVariable String id) {
        javniDokumenti.obrisi(id);
        return Map.of("ok", true);
    }

    @PostMapping("/obavjestenja")
    public Map<String, Object> dodajObavjestenje(@RequestParam String naslov,
                                                 @RequestParam(required = false) String tekst,
                                                 @RequestParam String vrsta,
                                                 @RequestParam(required = false) LocalDate datum,
                                                 @RequestParam(defaultValue = "false") boolean vazno,
                                                 @RequestParam(value = "prilog", required = false) MultipartFile prilog) {
        obavjestenjaService.dodaj(naslov, tekst, vrsta, datum, vazno, prilog);
        return Map.of("ok", true);
    }

    @PostMapping("/obavjestenja/{id}/izmijeni")
    public Map<String, Object> izmijeniObavjestenje(@PathVariable String id,
                                                    @RequestParam String naslov,
                                                    @RequestParam(required = false) String tekst,
                                                    @RequestParam String vrsta,
                                                    @RequestParam(required = false) LocalDate datum,
                                                    @RequestParam(defaultValue = "false") boolean vazno,
                                                    @RequestParam(value = "prilog", required = false) MultipartFile prilog,
                                                    @RequestParam(defaultValue = "false") boolean ukloniPrilog) {
        obavjestenjaService.izmijeni(id, naslov, tekst, vrsta, datum, vazno, prilog, ukloniPrilog);
        return Map.of("ok", true);
    }

    @PostMapping("/obavjestenja/{id}/obrisi")
    public Map<String, Object> obrisiObavjestenje(@PathVariable String id) {
        obavjestenjaService.obrisi(id);
        return Map.of("ok", true);
    }
}