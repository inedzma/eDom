package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Šifra studenta: SC-00001/26.
 *  - broj (00001) je trajan, čuva se u student.sifra_broj
 *  - godina (/26) dolazi iz prijave, pa se svake akademske godine sama mijenja
 */
@Component
public class SifraStudenta {

    @PersistenceContext
    private EntityManager em;

    /** Pozvati prije snimanja novog studenta. Postojeći broj se nikad ne mijenja. */
    public void dodijeli(Student s) {
        if (s.getSifraBroj() == null) {
            s.setSifraBroj(sljedeciBroj());
        }
    }

    /** Pravilo dodjele: najveći postojeći + 1. Ovdje se mijenja ako dom odluči drugačije. */
    public int sljedeciBroj() {
        Number max = (Number) em
                .createNativeQuery("SELECT COALESCE(MAX(sifra_broj), 0) FROM student")
                .getSingleResult();
        return max.intValue() + 1;
    }

    /**
     * Šifra za profil: s godinom zadnje prijave (SC-00001/26),
     * ili bez godine (SC-00001) ako student još nema prijavu. Null ako nema broj.
     */
    @Transactional(readOnly = true)
    public String zaStudenta(Integer idStudenta) {
        List<Integer> brojevi = em
                .createQuery("SELECT s.sifraBroj FROM Student s WHERE s.idStudent = :id", Integer.class)
                .setParameter("id", idStudenta)
                .getResultList();
        Integer broj = brojevi.isEmpty() ? null : brojevi.get(0);
        if (broj == null) return null;

        Integer godina = em
                .createQuery("SELECT MAX(p.akademskaGodina) FROM Prijava p WHERE p.student.idStudent = :id",
                        Integer.class)
                .setParameter("id", idStudenta)
                .getSingleResult();
        return format(broj, godina);
    }

    /** Izgled šifre: SC- + broj na 5 cifara + / + zadnje dvije cifre akademske godine. */
    public static String format(Integer broj, Integer akademskaGodina) {
        if (broj == null) return "—";
        String godina = akademskaGodina == null ? "" : String.format("/%02d", akademskaGodina % 100);
        return String.format("SC-%05d%s", broj, godina);
    }
}