package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.BodovanjeService;
import ba.unze.edom.server.service.PrijavaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/student/prijava")
@RequiredArgsConstructor
public class PrijavaWebController {

    private final PrijavaService prijavaService;
    private final BodovanjeService bodovanjeService;

    // ---------- KREIRANJE ----------

    @GetMapping("/nova")
    public String nova(@AuthenticationPrincipal KorisnikPrincipal k,
                       RedirectAttributes ra) {
        try {
            int godina = LocalDate.now().getYear();
            var p = prijavaService.kreirajIliNastavi(k.getIdStudenta(), godina);
            return "redirect:/student/prijava/" + p.getIdPrijava() + "/studij";
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            return "redirect:/student/pocetna";
        }
    }

    // ---------- KORAK 1 ----------

    @GetMapping("/{id}/studij")
    public String studij(@PathVariable Integer id,
                         @AuthenticationPrincipal KorisnikPrincipal k,
                         Model model) {

        var p = prijavaService.dohvatiSvoju(id, k.getIdStudenta());

        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 1);
        model.addAttribute("forma", new StudijKorakDTO(
                p.getAkademskaGodina(), p.getGodinaStudija(),
                p.getProsjek(), p.getPolozeniIspiti()));

        return "student/prijava/studij";
    }

    @PostMapping("/{id}/studij")
    public String sacuvajStudij(@PathVariable Integer id,
                                @AuthenticationPrincipal KorisnikPrincipal k,
                                @Valid @ModelAttribute("forma") StudijKorakDTO forma,
                                BindingResult greske,
                                Model model) {

        if (greske.hasErrors()) {
            model.addAttribute("idPrijave", id);
            model.addAttribute("korak", 1);
            return "student/prijava/studij";
        }

        try {
            prijavaService.sacuvajStudij(id, k.getIdStudenta(), forma);
            return "redirect:/student/prijava/" + id + "/domacinstvo";
        } catch (RuntimeException e) {
            model.addAttribute("idPrijave", id);
            model.addAttribute("korak", 1);
            model.addAttribute("greska", e.getMessage());
            return "student/prijava/studij";
        }
    }

    // ---------- KORAK 2 ----------

    @GetMapping("/{id}/domacinstvo")
    public String domacinstvo(@PathVariable Integer id,
                              @AuthenticationPrincipal KorisnikPrincipal k,
                              Model model) {

        var p = prijavaService.dohvatiSvoju(id, k.getIdStudenta());

        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 2);
        model.addAttribute("forma", new DomacinstvoKorakDTO(
                p.getBrojClanovaDomacinstva(), p.getUkupnaPrimanja(),
                p.getUdaljenostKm()));

        return "student/prijava/domacinstvo";
    }

    @PostMapping("/{id}/domacinstvo")
    public String sacuvajDomacinstvo(@PathVariable Integer id,
                                     @AuthenticationPrincipal KorisnikPrincipal k,
                                     @Valid @ModelAttribute("forma") DomacinstvoKorakDTO forma,
                                     BindingResult greske,
                                     Model model) {

        if (greske.hasErrors()) {
            model.addAttribute("idPrijave", id);
            model.addAttribute("korak", 2);
            return "student/prijava/domacinstvo";
        }

        try {
            prijavaService.sacuvajDomacinstvo(id, k.getIdStudenta(), forma);
            return "redirect:/student/prijava/" + id + "/kriteriji";
        } catch (RuntimeException e) {
            model.addAttribute("idPrijave", id);
            model.addAttribute("korak", 2);
            model.addAttribute("greska", e.getMessage());
            return "student/prijava/domacinstvo";
        }
    }

    // ---------- KORAK 3 ----------

    @GetMapping("/{id}/kriteriji")
    public String kriteriji(@PathVariable Integer id,
                            @AuthenticationPrincipal KorisnikPrincipal k,
                            Model model) {

        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 3);
        model.addAttribute("forma",
                prijavaService.pripremiKriterije(id, k.getIdStudenta()));

        return "student/prijava/kriteriji";
    }

    @PostMapping("/{id}/kriteriji")
    public String sacuvajKriterije(@PathVariable Integer id,
                                   @AuthenticationPrincipal KorisnikPrincipal k,
                                   @ModelAttribute("forma") KriterijiFormaDTO forma,
                                   Model model) {
        try {
            prijavaService.sacuvajKriterije(id, k.getIdStudenta(), forma);
            return "redirect:/student/prijava/" + id + "/pregled";
        } catch (RuntimeException e) {
            model.addAttribute("idPrijave", id);
            model.addAttribute("korak", 3);
            model.addAttribute("greska", e.getMessage());
            model.addAttribute("forma",
                    prijavaService.pripremiKriterije(id, k.getIdStudenta()));
            return "student/prijava/kriteriji";
        }
    }

    // ---------- KORAK 4: PREGLED ----------

    @GetMapping("/{id}/pregled")
    public String pregled(@PathVariable Integer id,
                          @AuthenticationPrincipal KorisnikPrincipal k,
                          Model model) {

        var p = prijavaService.dohvatiSvoju(id, k.getIdStudenta());

        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 4);
        model.addAttribute("prijava", p);
        model.addAttribute("rezultat", bodovanjeService.izracunaj(p));

        return "student/prijava/pregled";
    }

    @PostMapping("/{id}/podnesi")
    public String podnesi(@PathVariable Integer id,
                          @AuthenticationPrincipal KorisnikPrincipal k,
                          RedirectAttributes ra) {
        try {
            prijavaService.podnesi(id, k.getIdStudenta());
            ra.addFlashAttribute("uspjeh", "Prijava je uspješno podnesena.");
            return "redirect:/student/pocetna";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            return "redirect:/student/prijava/" + id + "/pregled";
        }
    }

    // ---------- ODUSTAJANJE ----------

    @PostMapping("/{id}/odustani")
    public String odustani(@PathVariable Integer id,
                           @AuthenticationPrincipal KorisnikPrincipal k,
                           RedirectAttributes ra) {
        try {
            prijavaService.odustani(id, k.getIdStudenta());
            ra.addFlashAttribute("uspjeh", "Prijava je obrisana.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("greska", e.getMessage());
        }
        return "redirect:/student/pocetna";
    }
}