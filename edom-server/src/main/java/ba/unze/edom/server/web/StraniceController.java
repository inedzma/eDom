package ba.unze.edom.server.web;

import ba.unze.edom.common.dto.RangObjavaDTO;
import ba.unze.edom.server.dto.KontaktForma;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.security.Uloge;
import ba.unze.edom.server.service.GalerijaService;
import ba.unze.edom.server.service.ObavjestenjaService;
import ba.unze.edom.server.service.ProfilService;
import ba.unze.edom.server.service.RangListaObjavaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StraniceController {

    private static final Logger log = LoggerFactory.getLogger(StraniceController.class);

    private final RangListaObjavaService objavaService;
    private final GalerijaService galerijaService;
    private final ProfilService profilService;
    private final ObavjestenjaService obavjestenjaService;

    /** Student ide na svoj portal; admin ostaje na javnoj Početnoj (uređivanje teksta). */
    @GetMapping("/")
    public String pocetna(Authentication auth, Model model) {
        if (prijavljen(auth) && !Uloge.jeAdmin(auth)) {
            return "redirect:/student/pocetna";
        }
        model.addAttribute("objava", objava());
        model.addAttribute("zadnjaObavjestenja", obavjestenjaService.zadnja(3));
        return "pocetna";
    }

    /** Već prijavljen korisnik nema šta tražiti na login stranici. */
    @GetMapping("/login")
    public String login(Authentication auth) {
        if (prijavljen(auth)) {
            return Uloge.jeAdmin(auth) ? "redirect:/" : "redirect:/student/pocetna";
        }
        return "login";
    }

    /** Javna stranica: smještaj, ishrana, sadržaji i galerija. */
    @GetMapping("/usluge")
    public String usluge(Model model) {
        model.addAttribute("galerija", galerijaService.lista());
        model.addAttribute("galerijaMax", GalerijaService.MAX_SLIKA);
        return "usluge";
    }

    /** Ako se objava ne može pročitati, Početna se ipak prikaže ("još nije objavljena"). */
    private RangObjavaDTO objava() {
        try {
            return objavaService.zadnjaInfo();
        } catch (RuntimeException e) {
            log.warn("Objava rang liste za Početnu nije dostupna: {}", e.getMessage());
            return RangObjavaDTO.nema();
        }
    }

    private static boolean prijavljen(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** Javna stranica Kontakt; prijavljenom studentu forma je već popunjena imenom i e-mailom. */
    /** Javna stranica Kontakt; prijavljenom studentu forma je već popunjena imenom i e-mailom. */
    @GetMapping("/kontakt")
    public String kontakt(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {
        if (!model.containsAttribute("forma") && korisnik != null && korisnik.getIdStudenta() != null) {
            try {
                var p = profilService.ucitaj(korisnik.getIdStudenta());
                model.addAttribute("forma", new KontaktForma(p.ime(), p.email(), null, null, null));
            } catch (RuntimeException e) {
                log.warn("Profil za kontakt formu nije učitan: {}", e.getMessage());
            }
        }
        return "kontakt";
    }

    /** Javna stranica: historija, misija, vizija. */
    @GetMapping("/o-ustanovi")
    public String oUstanovi() {
        return "o-ustanovi";
    }
}