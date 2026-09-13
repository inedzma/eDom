package ba.unze.edom.server.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SigurnostTest {

    @Autowired MockMvc mockMvc;

    // ---------- javne rute ----------

    @Test
    @DisplayName("Pocetna je javna")
    void pocetnaJavna() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login je javan")
    void loginJavan() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Registracija je javna")
    void registracijaJavna() throws Exception {
        mockMvc.perform(get("/registracija")).andExpect(status().isOk());
    }

    // ---------- zasticene rute ----------

    @Test
    @DisplayName("Neprijavljen ne moze na studentske stranice")
    void neprijavljenNeMoze() throws Exception {
        mockMvc.perform(get("/student/pocetna"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "Admin")
    @DisplayName("Admin ne moze na studentske stranice")
    void adminNeMozeNaStudentske() throws Exception {
        mockMvc.perform(get("/student/pocetna"))
                .andExpect(status().isForbidden());
    }

    // ---------- API lanac ----------

    @Test
    @DisplayName("API bez tokena vraca 401")
    void apiBezTokena() throws Exception {
        mockMvc.perform(get("/api/studenti"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "Student")
    @DisplayName("Student ne moze na admin API")
    void studentNeMozeNaApi() throws Exception {
        mockMvc.perform(get("/api/studenti"))
                .andExpect(status().isForbidden());
    }
}