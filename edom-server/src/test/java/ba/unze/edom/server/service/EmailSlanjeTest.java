package ba.unze.edom.server.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Stvarno salje e-mail. Ne pokrece se automatski.
 * Za rucno pokretanje: makni @Disabled i upisi svoju adresu.
 */
@SpringBootTest
@ActiveProfiles("mail")
@Disabled("Rucni test - salje pravi e-mail")
class EmailSlanjeTest {

    @Autowired EmailService emailService;

    @Test
    @DisplayName("Slanje reset koda na pravu adresu")
    void posaljiKod() {
        emailService.posaljiResetKod("TVOJA.ADRESA@example.com", "123456");
        // provjeri inbox
    }
}