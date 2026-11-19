package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.uplate.PodsjetnikUplateDTO;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO;
import ba.unze.edom.server.service.uplate.UplataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/** Uplate za smještaj (samo admin; zaštita u SecurityConfig za /api/**). */
@RestController
@RequestMapping("/api/uplate")
@RequiredArgsConstructor
public class UplataApiController {

    private final UplataService uplataService;

    /** GET /api/uplate?godina=2026 (bez godine: tekuća akademska godina iz Postavki) */
    @GetMapping
    public UplateTabelaDTO tabela(@RequestParam(required = false) Integer godina) {
        return uplataService.tabela(godina);
    }

    /** POST /api/uplate/15/2026-10?datum=2026-10-03 (bez datuma: danas) */
    @PostMapping("/{idStudenta}/{mjesec}")
    public Map<String, Boolean> evidentiraj(@PathVariable Integer idStudenta,
                                            @PathVariable String mjesec,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        uplataService.evidentiraj(idStudenta, YearMonth.parse(mjesec), datum);
        return Map.of("ok", true);
    }

    /** POST /api/uplate/podsjetnik  { akademskaGodina, idStudenti, napomena } */
    @PostMapping("/podsjetnik")
    public UplataService.PodsjetnikRezultat podsjetnik(@RequestBody PodsjetnikUplateDTO z) {
        return uplataService.posaljiPodsjetnik(z.akademskaGodina(), z.idStudenti(), z.napomena());
    }

    /** POST /api/uplate/15/2026-10/obrisi */
    @PostMapping("/{idStudenta}/{mjesec}/obrisi")
    public Map<String, Boolean> ponisti(@PathVariable Integer idStudenta, @PathVariable String mjesec) {
        uplataService.ponisti(idStudenta, YearMonth.parse(mjesec));
        return Map.of("ok", true);
    }
}