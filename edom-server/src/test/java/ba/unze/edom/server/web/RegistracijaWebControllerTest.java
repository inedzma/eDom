package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.service.korisnik.RegistracijaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// Ako se ovaj import crveni, kopiraj import za @AutoConfigureMockMvc iz SigurnostTest.java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Forma za registraciju kroz cijeli web sloj: Spring Security (CSRF),
 * kontroler, validacija i Thymeleaf stranica. Šalje se isti zahtjev
 * kao kad student klikne "Registruj se", ali bez preglednika.
 *
 * RegistracijaService je zamijenjen Mockito objektom, pa se u bazu ništa ne upisuje.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistracijaWebControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean RegistracijaService registracijaService;

    @Test
    @DisplayName("Stranica za registraciju se otvara bez prijave")
    void otvaranjeForme() throws Exception {
        mvc.perform(get("/registracija"))
                .andExpect(status().isOk())
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeExists("zahtjev"));
    }

    @Test
    @DisplayName("Ispravna forma: nalog se pravi i student ide na prijavu")
    void ispravnaForma() throws Exception {
        mvc.perform(forma())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrovan"));

        verify(registracijaService).registruj(any(RegistracijaZahtjev.class));
    }

    @Test
    @DisplayName("Brucoš (1. godina) se može registrovati bez broja indeksa")
    void brucosBezIndeksa() throws Exception {
        mvc.perform(forma("godinaStudija", "1", "brojIndeksa", ""))
                .andExpect(redirectedUrl("/login?registrovan"));
    }

    @Test
    @DisplayName("Student 2. ili starije godine mora upisati broj indeksa")
    void starijiBezIndeksa() throws Exception {
        mvc.perform(forma("godinaStudija", "3", "brojIndeksa", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "brojIndeksa"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Prazno ime: forma se vraća s porukom, nalog se ne pravi")
    void praznoIme() throws Exception {
        mvc.perform(forma("ime", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "ime"))
                .andExpect(content().string(containsString("Ime je obavezno")));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Više praznih polja: greška se prikazuje kod svakog")
    void viseGresaka() throws Exception {
        mvc.perform(forma("prezime", "", "jmbg", "", "adresa", ""))
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "prezime", "jmbg", "adresa"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Pogrešan e-mail se odbija")
    void pogresanEmail() throws Exception {
        mvc.perform(forma("email", "merjem@"))
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "email"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Slaba lozinka se odbija")
    void slabaLozinka() throws Exception {
        mvc.perform(forma("lozinka", "lozinka123"))
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "lozinka"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Neoznačena saglasnost: registracija nije moguća")
    void bezSaglasnosti() throws Exception {
        // neoznačen checkbox preglednik uopšte ne šalje
        mvc.perform(forma("saglasnost", null))
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "saglasnost"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Godina studija koja nije broj se odbija")
    void godinaNijeBroj() throws Exception {
        mvc.perform(forma("godinaStudija", "treća"))
                .andExpect(view().name("registracija"))
                .andExpect(model().attributeHasFieldErrors("zahtjev", "godinaStudija"));

        verifyNoInteractions(registracijaService);
    }

    @Test
    @DisplayName("Greška iz servisa (npr. zauzet e-mail) prikazuje se na formi")
    void greskaIzServisa() throws Exception {
        doThrow(new RegistracijaException("Ta e-mail adresa je već u upotrebi."))
                .when(registracijaService).registruj(any());

        mvc.perform(forma())
                .andExpect(status().isOk())
                .andExpect(view().name("registracija"))
                .andExpect(model().attribute("greska", "Ta e-mail adresa je već u upotrebi."));
    }

    @Test
    @DisplayName("Forma poslana bez CSRF tokena se odbija i korisnik dobija poruku da je sesija istekla")
    void bezCsrf() throws Exception {
        mvc.perform(post("/registracija").param("ime", "Merjem"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("istekla")));

        verifyNoInteractions(registracijaService);
    }
    // ---------- pomoćne ----------

    /** Ispravno popunjena forma, kao kad je student pošalje iz preglednika. */
    private static final Map<String, String> ISPRAVNO = Map.ofEntries(
            Map.entry("ime", "Merjem"),
            Map.entry("prezime", "Grizić"),
            Map.entry("jmbg", "0101003175007"),
            Map.entry("brojIndeksa", "320"),
            Map.entry("email", "merjem@example.com"),
            Map.entry("telefon", "061111222"),
            Map.entry("fakultet", "Drugo"),
            Map.entry("godinaStudija", "3"),
            Map.entry("adresa", "Zenica, Ulica 1"),
            Map.entry("imeRoditelja", "Adem"),
            Map.entry("lozinka", "NovaLozinka1!"),
            Map.entry("saglasnost", "true"));

    /**
     * Ispravna forma u kojoj se mijenjaju samo navedena polja: forma("ime", "", "jmbg", "123").
     * Vrijednost null znači da preglednik to polje uopšte ne šalje (npr. neoznačen checkbox).
     */
    private static MockHttpServletRequestBuilder forma(String... izmjene) {
        Map<String, String> polja = new HashMap<>(ISPRAVNO);
        for (int i = 0; i < izmjene.length; i += 2) {
            polja.put(izmjene[i], izmjene[i + 1]);
        }
        MockHttpServletRequestBuilder zahtjev = post("/registracija").with(csrf());
        polja.forEach((k, v) -> { if (v != null) zahtjev.param(k, v); });
        return zahtjev;
    }
}