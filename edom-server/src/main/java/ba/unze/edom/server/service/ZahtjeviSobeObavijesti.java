package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.ObavijestDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.entity.ZahtjevPromjeneSobe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Zahtjevi za promjenu sobe koji čekaju odgovor administracije. */
@Component
@Order(2)
public class ZahtjeviSobeObavijesti implements IzvorObavijesti {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ObavijestDTO> obavijesti() {
        List<ZahtjevPromjeneSobe> zahtjevi = em.createQuery(
                        "SELECT z FROM ZahtjevPromjeneSobe z JOIN FETCH z.prijava p JOIN FETCH p.student "
                                + "WHERE z.status = :status ORDER BY z.datumPodnosenja DESC", ZahtjevPromjeneSobe.class)
                .setParameter("status", ZahtjevPromjeneSobe.NA_CEKANJU)
                .getResultList();

        return zahtjevi.stream().map(this::uObavijest).toList();
    }

    private ObavijestDTO uObavijest(ZahtjevPromjeneSobe z) {
        Prijava p = z.getPrijava();
        Student s = p.getStudent();
        boolean zamjena = ZahtjevPromjeneSobe.ZAMJENA.equals(z.getVrsta());

        String detalj = zamjena
                ? "Zamjena s: " + (z.getSaStudentom() != null ? z.getSaStudentom() : "—")
                : (z.getZeljenaSoba() != null ? "Željena soba: " + z.getZeljenaSoba() : "Premještaj u drugu sobu");

        return new ObavijestDTO(
                "ZAHTJEV_SOBA",
                "ZAHTJEV_SOBA:" + z.getIdZahtjev(),
                (zamjena ? "Zamjena sobe: " : "Premještaj: ") + s.getIme() + " " + s.getPrezime(),
                detalj + "  ·  " + skrati(z.getRazlog(), 120),
                dan(z.getDatumPodnosenja()),
                p.getIdPrijava(),
                null);
    }

    // radi i ako je datum podnošenja LocalDateTime i ako je LocalDate
    private static LocalDate dan(LocalDateTime t) { return t != null ? t.toLocalDate() : null; }
    private static LocalDate dan(LocalDate d) { return d; }

    private static String skrati(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}