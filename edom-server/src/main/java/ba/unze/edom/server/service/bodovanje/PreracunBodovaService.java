package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.service.opste.PostavkeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Poslije promjene pravila ili kriterija: ukupni_bodovi svih prijava godine konkursa iznova. */
@Service
@RequiredArgsConstructor
public class PreracunBodovaService {

    @PersistenceContext
    private EntityManager em;

    private final BodovanjeService bodovanjeService;
    private final PostavkeService postavke;

    @Transactional
    public int preracunaj() {
        int n = 0;
        for (Prijava p : em.createQuery("SELECT p FROM Prijava p WHERE p.akademskaGodina = :g", Prijava.class)
                .setParameter("g", postavke.akademskaGodina())
                .getResultList()) {
            try {
                p.setUkupniBodovi(bodovanjeService.izracunaj(p).potvrdjeno());
                n++;
            } catch (NevalidnaPrijavaException e) {
                // neispravni podaci: ostaje stari broj, admin ih vidi u detaljima
            }
        }
        return n;
    }
}