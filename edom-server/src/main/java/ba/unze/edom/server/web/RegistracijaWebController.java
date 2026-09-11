package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.service.RegistracijaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class RegistracijaWebController {

    private final RegistracijaService registracijaService;

    @GetMapping("/registracija")
    public String prikaziFormu(Model model) {
        model.addAttribute("zahtjev", prazanZahtjev());
        return "registracija";
    }

    @PostMapping("/registracija")
    public String obradiFormu(@Valid @ModelAttribute("zahtjev") RegistracijaZahtjev zahtjev,
                              BindingResult greske,
                              Model model) {

        if (greske.hasErrors()) {
            return "registracija";
        }

        try {
            registracijaService.registruj(zahtjev);
            return "redirect:/login?registrovan";
        } catch (RegistracijaException e) {
            model.addAttribute("greska", e.getMessage());
            return "registracija";
        }
    }

    private RegistracijaZahtjev prazanZahtjev() {
        return new RegistracijaZahtjev(
                "", "", "", "", "", "", "", null, "", "", "");
    }
}