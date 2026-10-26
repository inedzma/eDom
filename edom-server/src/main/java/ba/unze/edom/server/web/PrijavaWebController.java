package ba.unze.edom.server.web;

import ba.unze.edom.common.util.Opcine;
import ba.unze.edom.server.dto.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.BodovanjeService;
import ba.unze.edom.server.service.PotrebniDokumentiService;
import ba.unze.edom.server.service.PrijavaService;
import ba.unze.edom.server.service.DokumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/student/prijava")
@RequiredArgsConstructor
public class PrijavaWebController {

    private final PrijavaService prijavaService;
    private final BodovanjeService bodovanjeService;
    private final PotrebniDokumentiService potrebniDokumentiService;

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

    // ---------- KORAK 1 ----------

    @GetMapping("/{id}/studij")
    public String studij(@PathVariable Integer id,
                         @AuthenticationPrincipal KorisnikPrincipal k,
                         Model model) {

        modelStudij(model, id);
        model.addAttribute("forma", prijavaService.pripremiStudij(id, k.getIdStudenta()));
        return "student/prijava/studij";
    }

    @PostMapping("/{id}/studij")
    public String sacuvajStudij(@PathVariable Integer id,
                                @AuthenticationPrincipal KorisnikPrincipal k,
                                @Valid @ModelAttribute("forma") StudijKorakDTO forma,
                                BindingResult greske,
                                Model model) {

        if (greske.hasErrors()) {
            modelStudij(model, id);
            return "student/prijava/studij";
        }

        try {
            prijavaService.sacuvajStudij(id, k.getIdStudenta(), forma);
            return "redirect:/student/prijava/" + id + "/domacinstvo";
        } catch (RuntimeException e) {
            modelStudij(model, id);
            model.addAttribute("greska", e.getMessage());
            return "student/prijava/studij";
        }
    }

    private void modelStudij(Model model, Integer id) {
        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 1);
        model.addAttribute("opcinePoKantonu", Opcine.PO_KANTONU);
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
            return "redirect:/student/prijava/" + id + "/dokumenti";
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
        model.addAttribute("korak", 5);

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

    @GetMapping("/{id}")
    public String detalji(@PathVariable Integer id,
                          @AuthenticationPrincipal KorisnikPrincipal k,
                          Model model) {

        var p = prijavaService.dohvatiSvoju(id, k.getIdStudenta());

        model.addAttribute("prijava", p);
        model.addAttribute("rezultat", bodovanjeService.izracunaj(p));
        model.addAttribute("uIzradi",
                p.getStatus() != null && "u izradi".equals(p.getStatus().getNaziv()));

        return "student/prijava/detalji";
    }

    private final DokumentService dokumentService;   // novo polje

    // ---------- PREGLED ----------

    @GetMapping("/{id}/dokumenti")
    public String dokumenti(@PathVariable Integer id,
                            @AuthenticationPrincipal KorisnikPrincipal k,
                            Model model) {

        var p = prijavaService.dohvatiSvoju(id, k.getIdStudenta());   // provjera vlasništva
        String status = p.getStatus() != null ? p.getStatus().getNaziv() : "";

        model.addAttribute("idPrijave", id);
        model.addAttribute("korak", 4);
        model.addAttribute("spisak", potrebniDokumentiService.zaPrijavu(id));
        model.addAttribute("uIzradi", "u izradi".equals(status));
        model.addAttribute("mozeMijenjati",
                "u izradi".equals(status) || "na pregledu".equals(status) || "nepotpuna".equals(status));

        return "student/prijava/dokumenti";
    }

    // ---------- DODAVANJE ----------

    @PostMapping("/{id}/dokumenti")
    public String dodajDokument(@PathVariable Integer id,
                                @AuthenticationPrincipal KorisnikPrincipal k,
                                @RequestParam Integer idVrste,
                                @RequestParam(required = false) String naziv,
                                @RequestParam MultipartFile fajl,
                                RedirectAttributes ra) {
        try {
            dokumentService.dodaj(id, k.getIdStudenta(), idVrste, fajl, naziv);
            ra.addFlashAttribute("uspjeh", "Dokument je prilozen.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("greska", e.getMessage());
        }
        return "redirect:/student/prijava/" + id + "/dokumenti";
    }

    // ---------- BRISANJE ----------

    @PostMapping("/{id}/dokumenti/{idDok}/obrisi")
    public String obrisiDokument(@PathVariable Integer id,
                                 @PathVariable Integer idDok,
                                 @AuthenticationPrincipal KorisnikPrincipal k,
                                 RedirectAttributes ra) {
        try {
            dokumentService.obrisi(idDok, k.getIdStudenta());
            ra.addFlashAttribute("uspjeh", "Dokument je obrisan.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("greska", e.getMessage());
        }
        return "redirect:/student/prijava/" + id + "/dokumenti";
    }

    // ---------- PREUZIMANJE ----------

    @GetMapping("/{id}/dokumenti/{idDok}/preuzmi")
    public ResponseEntity<byte[]> preuzmi(@PathVariable Integer id,
                                          @PathVariable Integer idDok,
                                          @AuthenticationPrincipal KorisnikPrincipal k) {

        String b64 = dokumentService.sadrzaj(idDok, k.getIdStudenta());
        byte[] podaci = java.util.Base64.getDecoder().decode(b64);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"dokument-" + idDok + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(podaci);
    }

    @PostMapping("/{id}/dopuna")
    public String posaljiDopunu(@PathVariable Integer id,
                                @AuthenticationPrincipal KorisnikPrincipal k,
                                RedirectAttributes ra) {
        try {
            prijavaService.posaljiDopunu(id, k.getIdStudenta());
            ra.addFlashAttribute("uspjeh", "Dopuna je poslana. Administracija će ponovo pregledati prijavu.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("greska", e.getMessage());
        }
        return "redirect:/student/pocetna";
    }
}