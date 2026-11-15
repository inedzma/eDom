package ba.unze.edom.server.web;

import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.UlogaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// isti import kao u RegistracijaWebControllerTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prijava na sistem, na oba načina:
 *  - web (student i admin u pregledniku): login forma, sesija, odjava;
 *  - desktop (admin): /api/auth/login vraća JWT token za /api/**.
 *
 * Test na početku sam napravi studenta i admina s poznatim lozinkama.
 * Zbog @Transactional oba naloga se na kraju poništavaju, pa baza ostaje ista.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrijavaKorisnikaTest {

    private static final String STUDENT = "test.student@example.com";
    private static final String ADMIN = "test.admin@example.com";
    private static final String LOZINKA = "NovaLozinka1!";

    @Autowired MockMvc mvc;
    @Autowired KorisnikRepository korisnikRepository;
    @Autowired UlogaRepository ulogaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void napraviNaloge() {
        nalog(STUDENT, "Student");
        nalog(ADMIN, "ADMIN");
    }

    private void nalog(String email, String uloga) {
        Korisnik k = new Korisnik();
        k.setIme("Test");
        k.setPrezime(uloga);
        k.setEmail(email);
        k.setUsername(email);
        k.setPasswordHash(passwordEncoder.encode(LOZINKA));
        k.setUloga(ulogaRepository.findByNaziv(uloga)
                .orElseThrow(() -> new IllegalStateException("U bazi nema uloge '" + uloga + "'")));
        korisnikRepository.save(k);
    }

    // =====================================================================
    // WEB PRIJAVA
    // =====================================================================

    @Nested
    @DisplayName("Web prijava")
    class Web {

        @Test
        @DisplayName("Student s ispravnim podacima je prijavljen kao Student")
        void studentIspravno() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password(LOZINKA))
                    .andExpect(authenticated().withUsername(STUDENT).withRoles("Student"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", not(containsString("error"))));
        }

        @Test
        @DisplayName("Admin s ispravnim podacima je prijavljen kao Admin")
        void adminIspravno() throws Exception {
            mvc.perform(formLogin("/login").user(ADMIN).password(LOZINKA))
                    .andExpect(authenticated().withUsername(ADMIN).withRoles("ADMIN"))
                    .andExpect(header().string("Location", not(containsString("error"))));
        }

        @Test
        @DisplayName("Pogrešna lozinka: povratak na login s porukom greške")
        void pogresnaLozinka() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password("PogresnaLozinka1!"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("Nepostojeći korisnik: ista poruka kao za pogrešnu lozinku")
        void nepostojeciKorisnik() throws Exception {
            mvc.perform(formLogin("/login").user("niko@example.com").password(LOZINKA))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("Prazna polja: prijava nije moguća")
        void praznaPolja() throws Exception {
            mvc.perform(formLogin("/login").user("").password(""))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("Prazna lozinka uz ispravan e-mail: prijava nije moguća")
        void praznaLozinka() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password(""))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("Lozinka razlikuje velika i mala slova")
        void velikaMalaSlova() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password("novalozinka1!"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Lozinka s razmakom na kraju se ne prihvata")
        void razmakULozinci() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password(LOZINKA + " "))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Lozinka admina ne otvara nalog studenta (svaki nalog ima svoju)")
        void tudjiNalog() throws Exception {
            mvc.perform(formLogin("/login").user(STUDENT).password("DrugaLozinka2#"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Forma bez CSRF tokena: korisnik nije prijavljen, dobija poruku da je forma istekla")
        void bezCsrf() throws Exception {
            mvc.perform(post("/login").param("username", STUDENT).param("password", LOZINKA))
                    .andExpect(unauthenticated())
                    .andExpect(header().string("Location", containsString("istekla")));
        }

        @Test
        @DisplayName("Odjava: sesija se gasi i korisnik ide na Početnu")
        void odjava() throws Exception {
            mvc.perform(logout())
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @WithMockUser(username = STUDENT, roles = "Student")
        @DisplayName("Već prijavljen student na /login ide na svoj portal")
        void studentNaLoginu() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(redirectedUrl("/student/pocetna"));
        }

        @Test
        @WithMockUser(username = ADMIN, roles = "ADMIN")
        @DisplayName("Već prijavljen admin na /login ide na Početnu")
        void adminNaLoginu() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Neprijavljen korisnik vidi login formu")
        void loginForma() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @WithMockUser(username = STUDENT, roles = "Student")
        @DisplayName("Student ne može uređivati javne stranice (/urednik)")
        void studentNeMozeUredjivati() throws Exception {
            mvc.perform(post("/urednik/tekstovi").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Neprijavljen korisnik ne može uređivati javne stranice")
        void anonimniNeMozeUredjivati() throws Exception {
            mvc.perform(post("/urednik/tekstovi").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    // =====================================================================
    // DESKTOP PRIJAVA (JWT)
    // =====================================================================

    @Nested
    @DisplayName("Prijava za desktop (JWT)")
    class Desktop {

        @Test
        @DisplayName("Admin dobija token s kojim može koristiti /api")
        void adminDobijaToken() throws Exception {
            String token = token(ADMIN, LOZINKA);
            assertFalse(token.isBlank());

            mvc.perform(get("/api/studenti").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Pogrešna lozinka: 401 i nema tokena")
        void pogresnaLozinka() throws Exception {
            mvc.perform(apiLogin(ADMIN, "PogresnaLozinka1!"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Nepostojeći korisnik: 401")
        void nepostojeci() throws Exception {
            mvc.perform(apiLogin("niko@example.com", LOZINKA))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Student ne može dobiti token za desktop aplikaciju")
        void studentNemaPristup() throws Exception {
            mvc.perform(apiLogin(STUDENT, LOZINKA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Prazna polja se odbijaju")
        void praznaPolja() throws Exception {
            mvc.perform(apiLogin("", ""))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Bez tokena /api je zatvoren: 401")
        void bezTokena() throws Exception {
            mvc.perform(get("/api/studenti"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Izmišljen token se odbija: 401")
        void lazniToken() throws Exception {
            mvc.perform(get("/api/studenti").header("Authorization", "Bearer abc.def.ghi"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Izmijenjen token (promijenjen jedan znak u potpisu) se odbija: 401")
        void izmijenjenToken() throws Exception {
            String token = token(ADMIN, LOZINKA);
            char zadnji = token.charAt(token.length() - 2);
            String izmijenjen = token.substring(0, token.length() - 2)
                    + (zadnji == 'A' ? 'B' : 'A') + token.charAt(token.length() - 1);

            mvc.perform(get("/api/studenti").header("Authorization", "Bearer " + izmijenjen))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Web sesija ne otvara /api: desktop mora koristiti token")
        void sesijaNeOtvaraApi() throws Exception {
            mvc.perform(get("/api/studenti").with(
                            org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.httpBasic(ADMIN, LOZINKA)))
                    .andExpect(status().isUnauthorized());
        }

        // ---------- pomoćne ----------

        private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
        apiLogin(String username, String lozinka) {
            return post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"lozinka\":\"" + lozinka + "\"}");
        }

        private String token(String username, String lozinka) throws Exception {
            String json = mvc.perform(apiLogin(username, lozinka))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.read(json, "$.token");
        }
    }
}