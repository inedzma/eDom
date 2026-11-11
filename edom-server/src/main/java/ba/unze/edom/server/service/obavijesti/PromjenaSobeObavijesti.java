package ba.unze.edom.server.service.obavijesti;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.entity.ZeljaSmjestaja;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Zahtjevi za promjenu sobe: želje koje je student izmijenio nakon što mu je soba dodijeljena.
 * Prikazuju se 30 dana od izmjene; administrator odgovara studentu e-mailom.
 */
@Component
@Order(2)
public class PromjenaSobeObavijesti implements IzvorObavijesti {

    private static final int DANA = 30;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<ObavijestDTO> obavijesti() {
        List<ZeljaSmjestaja> zelje = em.createQuery(
                        "SELECT z FROM ZeljaSmjestaja z JOIN FETCH z.prijava p JOIN FETCH p.student LEFT JOIN FETCH z.soba "
                                + "WHERE z.datumIzmjene >= :od "
                                + "AND EXISTS (SELECT sm FROM Smjestaj sm WHERE sm.prijava = p AND sm.status IN :statusi) "
                                + "ORDER BY z.datumIzmjene DESC", ZeljaSmjestaja.class)
                .setParameter("od", LocalDateTime.now().minusDays(DANA))
                .setParameter("statusi", List.of(Smjestaj.DODIJELJEN, Smjestaj.USELJEN))
                .getResultList();

        return zelje.stream().map(this::uObavijest).toList();
    }

    private ObavijestDTO uObavijest(ZeljaSmjestaja z) {
        Prijava p = z.getPrijava();
        Student s = p.getStudent();

        List<String> d = new ArrayList<>();
        if (z.getSoba() != null) d.add("željena soba " + z.getSoba().getBroj());
        if (z.getCimer1() != null) d.add("cimer " + z.getCimer1());
        if (z.getCimer2() != null) d.add("cimer " + z.getCimer2());
        if (z.getNapomena() != null) d.add(skrati(z.getNapomena(), 120));

        return new ObavijestDTO(
                "ZAHTJEV_SOBA",
                "ZAHTJEV_SOBA:" + z.getIdZelja() + ":" + z.getDatumIzmjene(),   // nova izmjena = nova obavijest
                "Promjena sobe: " + s.getIme() + " " + s.getPrezime(),
                String.join("  ·  ", d),
                z.getDatumIzmjene().toLocalDate(),
                p.getIdPrijava(),
                null);
    }

    private static String skrati(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}