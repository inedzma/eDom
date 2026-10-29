package ba.unze.edom.server.web;

import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.ProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Ime prijavljenog studenta za navigaciju (gornji desni ugao) na svim web stranicama.
 * Kontroler koji sam postavi "ime" i dalje ima prednost.
 * Samo web paket: REST API za desktop ovo ne poziva.
 */
@ControllerAdvice(basePackages = "ba.unze.edom.server.web")
@RequiredArgsConstructor
public class KorisnikWebAdvice {

    private final ProfilService profilService;


    @ModelAttribute("ime")
    public String ime(@AuthenticationPrincipal KorisnikPrincipal k) {
        if (k == null || k.getIdStudenta() == null) return null;
        try {
            return profilService.ucitaj(k.getIdStudenta()).ime();
        } catch (RuntimeException e) {
            return null;   // navigacija tada pokaže "Student", stranica radi normalno
        }
    }
}