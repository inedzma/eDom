package ba.unze.edom.server.dto;

import ba.unze.edom.server.entity.Student;
import java.math.BigDecimal;

/** Samo ono što treba tabeli. Bez JMBG-a, adrese, imena roditelja i kontakata. */
public record StudentListaDTO(
        Integer idStudent,
        String ime,
        String prezime,
        String brojIndeksa,
        String fakultet,
        Integer godinaStudija,
        BigDecimal prosjek,
        SocijalniStatusDTO socijalniStatus
) {
    public static StudentListaDTO iz(Student s) {
        var ss = s.getSocijalniStatus();
        return new StudentListaDTO(
                s.getIdStudent(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(),
                s.getFakultet(), s.getGodinaStudija(), s.getProsjek(),
                ss == null ? null : new SocijalniStatusDTO(ss.getIdStatus(), ss.getNaziv())
        );
    }
}