package ba.unze.edom.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProvjeraMaila {

    private final JavaMailSender mailSender;

    @Value("${edom.mail.provjeri-pri-startu:true}")
    private boolean provjeri;

    @EventListener(ApplicationReadyEvent.class)
    public void provjeriVezu() {

        if (!provjeri) return;

        if (!(mailSender instanceof JavaMailSenderImpl impl)) {
            log.warn("Slanje e-maila nije konfigurisano.");
            return;
        }

        try {
            impl.testConnection();
            log.info("SMTP veza ispravna ({}:{}, korisnik {})",
                    impl.getHost(), impl.getPort(), impl.getUsername());
        } catch (Exception e) {
            log.error("""

                    ====================================================
                     SMTP VEZA NE RADI - e-mailovi se nece slati!
                     Host: {}  Port: {}  Korisnik: {}
                     Razlog: {}

                     Provjeri:
                      - da je na Google nalogu ukljucena verifikacija
                        u dva koraka (bez nje app password ne postoji)
                      - da je app password unesen BEZ razmaka (16 znakova)
                      - da spring.mail.username odgovara nalogu na kojem
                        je app password napravljen
                    ====================================================
                    """,
                    impl.getHost(), impl.getPort(), impl.getUsername(),
                    e.getMessage());
        }
    }
}