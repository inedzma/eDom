package ba.unze.edom.server.web;

import ba.unze.edom.server.security.Uloge;
import ba.unze.edom.server.service.javna.WebTekstoviService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/** Svim web stranicama daje izmijenjene tekstove (t) i da li je prijavljen urednik. */
@ControllerAdvice(basePackages = "ba.unze.edom.server.web")
@RequiredArgsConstructor
public class WebTekstoviAdvice {

    /** PROVJERI: nazivi uloga moraju odgovarati onima iz SecurityConfig-a. */

    private final WebTekstoviService webTekstovi;

    @ModelAttribute("t")
    public Map<String, String> tekstovi() {
        return webTekstovi.sve();
    }

    @ModelAttribute("urednik")
    public boolean urednik() {
        return Uloge.jeAdmin(SecurityContextHolder.getContext().getAuthentication());
    }

    /** CSRF token se pravi PRIJE iscrtavanja stranice (inače forme pucaju za neprijavljene). */
    @ModelAttribute
    public void csrf(CsrfToken token) {
        if (token != null) token.getToken();
    }
}