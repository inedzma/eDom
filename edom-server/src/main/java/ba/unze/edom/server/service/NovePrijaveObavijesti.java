package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.ObavijestDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Prijave koje čekaju pregled; neispravni podaci → upozorenje. */
@Component
@Order(1)
@RequiredArgsConstructor
public class NovePrijaveObavijesti implements IzvorObavijesti {

    @PersistenceContext
    private EntityManager em;

    private final BodovanjeService bodovanjeService;

    @Override
    public List<ObavijestDTO> obavijesti() {
        List<Prijava> prijave = em.createQuery(
                        "SELECT p FROM Prijava p JOIN FETCH p.student "
                                + "WHERE p.status.naziv = :status "
                                + "ORDER BY p.datumPrijave DESC, p.idPrijava DESC", Prijava.class)
                .setParameter("status", Statusi.NA_PREGLEDU)
                .getResultList();

        return prijave.stream().map(this::uObavijest).toList();
    }

    private ObavijestDTO uObavijest(Prijava p) {
        Student s = p.getStudent();

        String upozorenje = null;
        try {
            bodovanjeService.izracunaj(p);
        } catch (NevalidnaPrijavaException e) {
            upozorenje = "Neispravni podaci: " + e.getMessage();
        }

        Integer godina = UslovUpisa.godinaStudija(p);
        String opis = SifraStudenta.format(s.getSifraBroj(), p.getAkademskaGodina())
                + "  ·  " + (godina != null ? godina + ". godina" : "godina nije unesena")
                + (UslovUpisa.jeBrucos(p) ? " (brucoš)" : "")
                + (s.getFakultet() != null ? "  ·  " + s.getFakultet() : "");

        return new ObavijestDTO(
                "NOVA_PRIJAVA",
                "PRIJAVA:" + p.getIdPrijava(),
                "Nova prijava: " + s.getIme() + " " + s.getPrezime(),
                opis,
                p.getDatumPrijave(),
                p.getIdPrijava(),
                upozorenje);
    }
}