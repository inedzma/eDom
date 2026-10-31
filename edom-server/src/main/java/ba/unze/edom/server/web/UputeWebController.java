package ba.unze.edom.server.web;

import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.korisnik.ProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Javna stranica "Kako se prijaviti". Prijavljeni student je vidi sa svojim navbarom. */
@Controller
@RequiredArgsConstructor
public class UputeWebController {

    private final ProfilService profilService;

    @GetMapping("/upute")
    public String upute(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {
        // navbarStudent prikazuje ime u gornjem desnom uglu
        if (korisnik != null && korisnik.getIdStudenta() != null) {
            model.addAttribute("ime", profilService.ucitaj(korisnik.getIdStudenta()).ime());
        }
        return "upute";
    }
}