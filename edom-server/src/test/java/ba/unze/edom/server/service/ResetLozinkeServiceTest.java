package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetLozinkeServiceTest {

    @Mock KorisnikRepository korisnikRepository;
    @Mock EmailService emailService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // brze u testu
    private ResetLozinkeService service;
    private Korisnik k;

    @BeforeEach
    void pripremi() {
        service = new ResetLozinkeService(korisnikRepository, encoder, emailService);
        k = new Korisnik();
        k.setEmail("student@example.com");
        k.setPasswordHash(encoder.encode("staralozinka"));
    }

    private void postaviKod(String kod, Instant istek) {
        k.setResetTokenHash(encoder.encode(kod));
        k.setResetTokenIstek(istek);
        k.setResetPokusaji(0);
        when(korisnikRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(k));
    }

    private Instant zaDeset() { return Instant.now().plus(10, ChronoUnit.MINUTES); }

    @Test
    @DisplayName("Nepostojeci e-mail ne salje nista i ne baca izuzetak")
    void nepostojeciEmail() {
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.zatraziKod("nema@example.com"));
        verify(emailService, never()).posaljiResetKod(any(), any());
    }

    @Test
    @DisplayName("Kod se u bazi cuva samo kao hash")
    void kodJeHesiran() {
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.of(k));
        ArgumentCaptor<String> poslani = ArgumentCaptor.forClass(String.class);

        service.zatraziKod("student@example.com");

        verify(emailService).posaljiResetKod(eq("student@example.com"), poslani.capture());
        assertNotEquals(poslani.getValue(), k.getResetTokenHash());
        assertTrue(encoder.matches(poslani.getValue(), k.getResetTokenHash()));
    }

    @Test
    @DisplayName("Ispravan kod mijenja lozinku i ponistava kod")
    void ispravanKod() {
        postaviKod("123456", zaDeset());

        service.promijeniLozinku("student@example.com", "123456",
                "novalozinka", "novalozinka");

        assertTrue(encoder.matches("novalozinka", k.getPasswordHash()));
        assertNull(k.getResetTokenHash());
    }

    @Test
    @DisplayName("Kod se ne moze iskoristiti dvaput")
    void jednokratan() {
        postaviKod("123456", zaDeset());
        service.promijeniLozinku("student@example.com", "123456",
                "novalozinka", "novalozinka");

        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "123456",
                        "drugalozinka", "drugalozinka"));
    }

    @Test
    @DisplayName("Istekao kod se odbija")
    void istekao() {
        postaviKod("123456", Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "123456",
                        "novalozinka", "novalozinka"));
        assertTrue(encoder.matches("staralozinka", k.getPasswordHash()));
    }

    @Test
    @DisplayName("Pogresan kod povecava brojac pokusaja")
    void pogresanKod() {
        postaviKod("123456", zaDeset());

        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "000000",
                        "novalozinka", "novalozinka"));
        assertEquals(1, k.getResetPokusaji());
    }

    @Test
    @DisplayName("Poslije 5 pokusaja ni ispravan kod ne prolazi")
    void previsePokusaja() {
        postaviKod("123456", zaDeset());
        k.setResetPokusaji(ResetLozinkeService.MAX_POKUSAJA);

        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "123456",
                        "novalozinka", "novalozinka"));
        assertTrue(encoder.matches("staralozinka", k.getPasswordHash()));
    }

    @Test
    @DisplayName("Nepostojeci nalog i pogresan kod daju istu poruku")
    void istaPoruka() {
        when(korisnikRepository.findByEmail("nema@example.com"))
                .thenReturn(Optional.empty());
        postaviKod("123456", zaDeset());

        var a = assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("nema@example.com", "123456", "x", "x"));
        var b = assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "000000", "x", "x"));

        assertEquals(a.getMessage(), b.getMessage());
    }

    @Test
    @DisplayName("Prekratka lozinka se odbija")
    void kratkaLozinka() {
        postaviKod("123456", zaDeset());
        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "123456",
                        "kratka", "kratka"));
    }

    @Test
    @DisplayName("Lozinke koje se ne podudaraju se odbijaju")
    void nePodudaraju() {
        postaviKod("123456", zaDeset());
        assertThrows(RegistracijaException.class, () ->
                service.promijeniLozinku("student@example.com", "123456",
                        "novalozinka", "drugacija1"));
    }
}