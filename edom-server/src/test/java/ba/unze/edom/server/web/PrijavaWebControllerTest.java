package ba.unze.edom.server.web;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.bodovanje.BodovanjeService;
import ba.unze.edom.server.service.dokumenti.DokumentService;
import ba.unze.edom.server.service.dokumenti.PotrebniDokumentiService;
import ba.unze.edom.server.service.opste.PostavkeService;
import ba.unze.edom.server.service.prijava.ObrazacPrijaveService;
import ba.unze.edom.server.service.prijava.PrijavaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// isti import kao u RegistracijaWebControllerTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web prijava studenta kroz kontroler: ko smije pristupiti, šta se dešava
 * kad korak uspije ili ne uspije, prilaganje dokumenata i podnošenje.
 *
 * Servisi su zamijenjeni Mockito objektima: ovdje se provjerava da li kontroler
 * ispravno prosljeđuje zahtjev i reaguje na grešku, a pravila prijave
 * testiraju PrijavaValidatorTest i BodovanjeServiceTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrijavaWebControllerTest {

    private static final int ID_STUDENTA = 7;
    private static final int ID_PRIJAVE = 15;
    private static final String URL = "/student/prijava/" + ID_PRIJAVE;

    @Autowired MockMvc mvc;

    @MockitoBean PrijavaService prijavaService;
    @MockitoBean BodovanjeService bodovanjeService;
    @MockitoBean PotrebniDokumentiService potrebniDokumentiService;
    @MockitoBean ObrazacPrijaveService obrazacPrijaveService;
    @MockitoBean PostavkeService postavkeService;
    @MockitoBean DokumentService dokumentService;

    /** Prijavljen student s idStudenta = 7, kao poslije prave prijave na sistem. */
    private static RequestPostProcessor student() {
        return user(new KorisnikPrincipal(
                100, "student@example.com", "x", ID_STUDENTA,
                List.of(new SimpleGrantedAuthority("ROLE_Student"))));
    }

    // =====================================================================
    // PRISTUP
    // =====================================================================

    @Nested
    @DisplayName("Ko smije pristupiti")
    class Pristup {

        @Test
        @DisplayName("Neprijavljen korisnik ide na login")
        void anonimni() throws Exception {
            mvc.perform(get("/student/prijava/nova"))
                    .andExpect(redirectedUrl("/login"));
            verifyNoInteractions(prijavaService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Admin ne može popunjavati studentsku prijavu")
        void admin() throws Exception {
            mvc.perform(get("/student/prijava/nova"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(prijavaService);
        }

        @Test
        @DisplayName("Podnošenje bez CSRF tokena se odbija")
        void bezCsrf() throws Exception {
            mvc.perform(post(URL + "/podnesi").with(student()))
                    .andExpect(header().string("Location", containsString("istekla")));
            verifyNoInteractions(prijavaService);
        }

        @Test
        @DisplayName("Servis uvijek dobija id prijavljenog studenta, ne id iz zahtjeva")
        void idIzSesije() throws Exception {
            mvc.perform(post(URL + "/podnesi").with(student()).with(csrf())
                    .param("idStudenta", "999"));

            verify(prijavaService).podnesi(eq(ID_PRIJAVE), eq(ID_STUDENTA), any());
        }

        @Test
        @DisplayName("Tuđa prijava: servis odbija, student dobija poruku")
        void tudjaPrijava() throws Exception {
            doThrow(new PrijavaException("Prijava ne pripada vama."))
                    .when(prijavaService).podnesi(eq(ID_PRIJAVE), eq(ID_STUDENTA), any());

            mvc.perform(post(URL + "/podnesi").with(student()).with(csrf()))
                    .andExpect(flash().attribute("greska", "Prijava ne pripada vama."));
        }
    }

    // =====================================================================
    // NOVA PRIJAVA
    // =====================================================================

    @Nested
    @DisplayName("Nova prijava")
    class Nova {

        @Test
        @DisplayName("Nova ili započeta prijava: student ide na prvi korak")
        void nova() throws Exception {
            Prijava p = new Prijava();
            p.setIdPrijava(ID_PRIJAVE);
            when(postavkeService.akademskaGodina()).thenReturn(2026);
            when(prijavaService.kreirajIliNastavi(ID_STUDENTA, 2026)).thenReturn(p);

            mvc.perform(get("/student/prijava/nova").with(student()))
                    .andExpect(redirectedUrl(URL + "/studij"));
        }

        @Test
        @DisplayName("Konkurs zatvoren (ili prijava već postoji): povratak na početnu s porukom")
        void nijeMoguce() throws Exception {
            when(postavkeService.akademskaGodina()).thenReturn(2026);
            when(prijavaService.kreirajIliNastavi(ID_STUDENTA, 2026))
                    .thenThrow(new PrijavaException("Konkurs nije otvoren."));

            mvc.perform(get("/student/prijava/nova").with(student()))
                    .andExpect(redirectedUrl("/student/pocetna"))
                    .andExpect(flash().attribute("greska", "Konkurs nije otvoren."));
        }
    }

    // =====================================================================
    // PODNOŠENJE, ODUSTAJANJE, DOPUNA
    // =====================================================================

    @Nested
    @DisplayName("Podnošenje i odustajanje")
    class Podnosenje {

        @Test
        @DisplayName("Uspješno podnošenje: početna i poruka o uspjehu")
        void podnesi() throws Exception {
            mvc.perform(post(URL + "/podnesi").with(student()).with(csrf())
                            .param("napomena", "Dostavljam rješenje naknadno."))
                    .andExpect(redirectedUrl("/student/pocetna"))
                    .andExpect(flash().attribute("uspjeh", "Prijava je uspješno podnesena."));

            verify(prijavaService).podnesi(ID_PRIJAVE, ID_STUDENTA, "Dostavljam rješenje naknadno.");
        }

        @Test
        @DisplayName("Podnošenje bez napomene je dozvoljeno")
        void podnesiBezNapomene() throws Exception {
            mvc.perform(post(URL + "/podnesi").with(student()).with(csrf()))
                    .andExpect(redirectedUrl("/student/pocetna"));

            verify(prijavaService).podnesi(ID_PRIJAVE, ID_STUDENTA, null);
        }

        @Test
        @DisplayName("Neuspješno podnošenje (npr. nedostaje dokument): povratak na pregled s porukom")
        void podnesiGreska() throws Exception {
            doThrow(new PrijavaException("Nedostaju obavezni dokumenti."))
                    .when(prijavaService).podnesi(anyInt(), anyInt(), any());

            mvc.perform(post(URL + "/podnesi").with(student()).with(csrf()))
                    .andExpect(redirectedUrl(URL + "/pregled"))
                    .andExpect(flash().attribute("greska", "Nedostaju obavezni dokumenti."));
        }

        @Test
        @DisplayName("Odustajanje briše prijavu")
        void odustani() throws Exception {
            mvc.perform(post(URL + "/odustani").with(student()).with(csrf()))
                    .andExpect(redirectedUrl("/student/pocetna"))
                    .andExpect(flash().attribute("uspjeh", "Prijava je obrisana."));

            verify(prijavaService).odustani(ID_PRIJAVE, ID_STUDENTA);
        }

        @Test
        @DisplayName("Odustajanje od već podnesene prijave nije moguće")
        void odustaniGreska() throws Exception {
            doThrow(new PrijavaException("Podnesena prijava se ne može obrisati."))
                    .when(prijavaService).odustani(ID_PRIJAVE, ID_STUDENTA);

            mvc.perform(post(URL + "/odustani").with(student()).with(csrf()))
                    .andExpect(redirectedUrl("/student/pocetna"))
                    .andExpect(flash().attribute("greska", "Podnesena prijava se ne može obrisati."));
        }

        @Test
        @DisplayName("Dopuna nepotpune prijave se šalje na ponovni pregled")
        void dopuna() throws Exception {
            mvc.perform(post(URL + "/dopuna").with(student()).with(csrf()))
                    .andExpect(redirectedUrl("/student/pocetna"))
                    .andExpect(flash().attributeExists("uspjeh"));

            verify(prijavaService).posaljiDopunu(ID_PRIJAVE, ID_STUDENTA);
        }

        @Test
        @DisplayName("Dopuna prijave koja nije nepotpuna se odbija")
        void dopunaGreska() throws Exception {
            doThrow(new PrijavaException("Prijava nije vraćena na dopunu."))
                    .when(prijavaService).posaljiDopunu(ID_PRIJAVE, ID_STUDENTA);

            mvc.perform(post(URL + "/dopuna").with(student()).with(csrf()))
                    .andExpect(flash().attribute("greska", "Prijava nije vraćena na dopunu."));
        }
    }

    // =====================================================================
    // DOKUMENTI
    // =====================================================================

    @Nested
    @DisplayName("Dokumenti")
    class Dokumenti {

        private final MockMultipartFile pdf = new MockMultipartFile(
                "fajl", "uvjerenje.pdf", "application/pdf", "%PDF-1.4 test".getBytes());

        @Test
        @DisplayName("Prilaganje dokumenta: fajl ide servisu, student dobija potvrdu")
        void dodaj() throws Exception {
            mvc.perform(multipart(URL + "/dokumenti").file(pdf)
                            .param("idVrste", "3").param("naziv", "Uvjerenje o prihodima")
                            .with(student()).with(csrf()))
                    .andExpect(redirectedUrl(URL + "/dokumenti"))
                    .andExpect(flash().attributeExists("uspjeh"));

            verify(dokumentService).dodaj(eq(ID_PRIJAVE), eq(ID_STUDENTA), eq(3), any(), eq("Uvjerenje o prihodima"));
        }

        @Test
        @DisplayName("Odbijen fajl (npr. pogrešan format ili prevelik): poruka greške")
        void dodajGreska() throws Exception {
            doThrow(new PrijavaException("Dozvoljeni su PDF, JPG i PNG fajlovi."))
                    .when(dokumentService).dodaj(anyInt(), anyInt(), anyInt(), any(), any());

            mvc.perform(multipart(URL + "/dokumenti").file(pdf)
                            .param("idVrste", "3").with(student()).with(csrf()))
                    .andExpect(redirectedUrl(URL + "/dokumenti"))
                    .andExpect(flash().attribute("greska", "Dozvoljeni su PDF, JPG i PNG fajlovi."));
        }

        @Test
        @DisplayName("Bez odabrane vrste dokumenta zahtjev se ne obrađuje")
        void bezVrste() throws Exception {
            mvc.perform(multipart(URL + "/dokumenti").file(pdf).with(student()).with(csrf()))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(dokumentService);
        }

        @Test
        @DisplayName("Brisanje dokumenta")
        void obrisi() throws Exception {
            mvc.perform(post(URL + "/dokumenti/99/obrisi").with(student()).with(csrf()))
                    .andExpect(redirectedUrl(URL + "/dokumenti"))
                    .andExpect(flash().attribute("uspjeh", "Dokument je obrisan."));

            verify(dokumentService).obrisi(99, ID_STUDENTA);
        }

        @Test
        @DisplayName("Preuzimanje vraća tačno sadržaj priloženog fajla")
        void preuzmi() throws Exception {
            byte[] sadrzaj = "%PDF-1.4 test".getBytes();
            when(dokumentService.sadrzaj(99, ID_STUDENTA))
                    .thenReturn(Base64.getEncoder().encodeToString(sadrzaj));

            mvc.perform(get(URL + "/dokumenti/99/preuzmi").with(student()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("dokument-99")))
                    .andExpect(content().bytes(sadrzaj));
        }
    }
}