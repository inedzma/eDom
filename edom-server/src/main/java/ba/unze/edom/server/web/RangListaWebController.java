package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.ObjavljenaRangLista;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.ProfilService;
import ba.unze.edom.server.service.RangListaObjavaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RangListaWebController {

    private final RangListaObjavaService objavaService;
    private final ProfilService profilService;

    /** Javna stranica: prikazuje zadnju OBJAVLJENU listu (šifre), ne listu uživo. */
    @GetMapping("/rang-lista")
    public String rangLista(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {
        ObjavljenaRangLista lista = objavaService.zadnja();   // null = još nije objavljena
        model.addAttribute("lista", lista);

        // stranica je javna – ovo se postavlja samo ako je student prijavljen
        if (korisnik != null && korisnik.getIdStudenta() != null) {
            model.addAttribute("mojId", korisnik.getIdStudenta());
            model.addAttribute("ime", profilService.ucitaj(korisnik.getIdStudenta()).ime());
        }
        return "rang-lista";
    }
}