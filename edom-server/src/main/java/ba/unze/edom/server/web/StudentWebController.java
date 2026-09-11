package ba.unze.edom.server.web;

import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.PrijavaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentWebController {

    private final PrijavaService prijavaService;

    @GetMapping("/pocetna")
    public String pocetna(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {

        model.addAttribute("ime", korisnik.getUsername());
        model.addAttribute("prijave",
                prijavaService.pregledZaStudenta(korisnik.getIdStudenta()));

        return "student/pocetna";
    }
}