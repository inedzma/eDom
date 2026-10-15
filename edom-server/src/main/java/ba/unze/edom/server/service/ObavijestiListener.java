package ba.unze.edom.server.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ObavijestiListener {

    private static final Logger log = LoggerFactory.getLogger(ObavijestiListener.class);

    private final EmailService emailService;

    @Value("${edom.web.url:http://localhost:8080}")
    private String webUrl;

    /** Izvršava se tek kad je prijava sigurno upisana u bazu. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void prijavaEvidentirana(PrijavaEvidentiranaEvent e) {
        try {
            emailService.posaljiObavijestORucnomUnosu(
                    e.email(), e.ime(), e.akademskaGodina(), e.noviNalog(), webUrl);
        } catch (Exception ex) {
            log.warn("Obavijest o prijavi nije poslana na {}: {}", e.email(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void statusPromijenjen(StatusPromijenjenEvent e) {
        try {
            emailService.posaljiPromjenuStatusa(e.email(), e.ime(), e.akademskaGodina(),
                    e.status(), e.napomena(), e.rezultat(), webUrl);
        } catch (Exception ex) {
            log.warn("Obavijest o statusu nije poslana na {}: {}", e.email(), ex.getMessage());
        }
    }
}