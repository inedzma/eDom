package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.RangListaDTO;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.ProfilService;
import ba.unze.edom.server.service.RangListaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RangListaWebController {

    private final RangListaService rangListaService;
    private final ProfilService profilService;

    @GetMapping("/rang-lista")
    public String rangLista(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {
        RangListaDTO lista = rangListaService.trenutna();

        model.addAttribute("akademskaGodina", lista.akademskaGodina());
        model.addAttribute("brucosi", lista.brucosi());
        model.addAttribute("visegodisnji", lista.visegodisnji());

        // stranica je javna – ovo se postavlja samo ako je student prijavljen
        if (korisnik != null && korisnik.getIdStudenta() != null) {
            model.addAttribute("mojId", korisnik.getIdStudenta());
            model.addAttribute("ime", profilService.ucitaj(korisnik.getIdStudenta()).ime());
        }
        return "rang-lista";
    }
}