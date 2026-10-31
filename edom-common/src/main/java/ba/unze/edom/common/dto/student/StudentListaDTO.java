package ba.unze.edom.common.dto.student;

import ba.unze.edom.common.dto.prijava.SocijalniStatusDTO;

import java.math.BigDecimal;

public record StudentListaDTO(
        Integer idStudent,
        String ime,
        String prezime,
        String brojIndeksa,
        String fakultet,
        Integer godinaStudija,
        BigDecimal prosjek,
        SocijalniStatusDTO socijalniStatus
) {}