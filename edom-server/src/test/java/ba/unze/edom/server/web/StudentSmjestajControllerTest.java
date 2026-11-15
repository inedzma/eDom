package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.sobe.StudentSmjestajService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// isti import kao u RegistracijaWebControllerTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Forma "Želje za smještaj" / "Promjena sobe" na stranici Moj smještaj, kroz web sloj:
 * pristup, CSRF, prenos polja iz forme u servis i poruke studentu.
 * Servis je Mockito objekat; pravila želje testira StudentSmjestajServiceTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentSmjestajControllerTest {

    private static final int ID_STUDENTA = 7;
    private static final String URL = "/student/smjestaj/zelja";

    @Autowired MockMvc mvc;
    @MockitoBean StudentSmjestajService service;

    private static RequestPostProcessor student() {
        return user(new KorisnikPrincipal(
                100, "student@example.com", "x", ID_STUDENTA,
                List.of(new SimpleGrantedAuthority("ROLE_Student"))));
    }

    @Test
    @DisplayName("Sačuvana želja: povratak na formu s porukom o uspjehu")
    void sacuvano() throws Exception {
        mvc.perform(post(URL).with(student()).with(csrf()).param("zeljeniKapacitet", "2"))
                .andExpect(redirectedUrl("/student/smjestaj#zelja"))
                .andExpect(flash().attribute("poruka", "Sačuvano. Administracija će razmotriti vaše želje."));
    }

    @Test
    @DisplayName("Sva polja iz forme stižu u servis, za prijavljenog studenta")
    void poljaIzForme() throws Exception {
        mvc.perform(post(URL).with(student()).with(csrf())
                .param("zeljeniKapacitet", "3")
                .param("zeljenaSoba", "105")
                .param("cimer1", "ZE-2026-015")
                .param("cimer2", "ana@example.com")
                .param("napomena", "Blizu kupatila"));

        ArgumentCaptor<ZeljaForma> c = ArgumentCaptor.forClass(ZeljaForma.class);
        verify(service).sacuvajZelju(eq(ID_STUDENTA), c.capture());
        ZeljaForma f = c.getValue();
        assertEquals(3, f.getZeljeniKapacitet());
        assertEquals(105, f.getZeljenaSoba());
        assertEquals("ZE-2026-015", f.getCimer1());
        assertEquals("ana@example.com", f.getCimer2());
        assertEquals("Blizu kupatila", f.getNapomena());
    }

    @Test
    @DisplayName("Odbijena želja: poruka greške, a uneseni podaci ostaju u formi")
    void odbijeno() throws Exception {
        doThrow(new PrijavaException("Soba vam je već dodijeljena. U napomenu upišite razlog promjene sobe."))
                .when(service).sacuvajZelju(eq(ID_STUDENTA), any());

        mvc.perform(post(URL).with(student()).with(csrf()).param("zeljeniKapacitet", "3"))
                .andExpect(redirectedUrl("/student/smjestaj#zelja"))
                .andExpect(flash().attribute("greska",
                        "Soba vam je već dodijeljena. U napomenu upišite razlog promjene sobe."))
                .andExpect(flash().attributeExists("zelja"));
    }

    @Test
    @DisplayName("Broj sobe upisan slovima se odbija prije servisa")
    void sobaNijeBroj() throws Exception {
        mvc.perform(post(URL).with(student()).with(csrf()).param("zeljenaSoba", "sto pet"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Neprijavljen korisnik ide na login")
    void anonimni() throws Exception {
        mvc.perform(post(URL).with(csrf()))
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin ne može slati studentsku želju")
    void admin() throws Exception {
        mvc.perform(post(URL).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Forma bez CSRF tokena se odbija")
    void bezCsrf() throws Exception {
        mvc.perform(post(URL).with(student()).param("zeljeniKapacitet", "2"))
                .andExpect(header().string("Location", containsString("istekla")));
        verifyNoInteractions(service);
    }
}