package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.ProfilIzmjenaDTO;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.PrijavaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ba.unze.edom.server.service.ProfilService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @ExceptionHandler(PrijavaException.class)
    public String prijavaGreska(PrijavaException e, RedirectAttributes ra) {
        ra.addFlashAttribute("greska", e.getMessage());
        return "redirect:/student/pocetna";
    }

    private final ProfilService profilService;

    @GetMapping("/profil")
    public String profil(@AuthenticationPrincipal KorisnikPrincipal korisnik, Model model) {
        var profil = profilService.ucitaj(korisnik.getIdStudenta());
        model.addAttribute("profil", profil);
        model.addAttribute("izmjena", new ProfilIzmjenaDTO(
                profil.email(), profil.telefon(), profil.adresa(),
                profil.fakultet(), profil.godinaStudija()));
        return "student/profil";
    }

    @PostMapping("/profil")
    public String sacuvajProfil(@AuthenticationPrincipal KorisnikPrincipal korisnik,
                                @Valid @ModelAttribute("izmjena") ProfilIzmjenaDTO izmjena,
                                BindingResult greske,
                                Model model) {

        if (greske.hasErrors()) {
            model.addAttribute("profil", profilService.ucitaj(korisnik.getIdStudenta()));
            return "student/profil";
        }

        try {
            profilService.sacuvaj(korisnik.getIdKorisnik(), korisnik.getIdStudenta(), izmjena);
            return "redirect:/student/profil?sacuvano";
        } catch (RegistracijaException e) {
            model.addAttribute("profil", profilService.ucitaj(korisnik.getIdStudenta()));
            model.addAttribute("greska", e.getMessage());
            return "student/profil";
        }
    }
}