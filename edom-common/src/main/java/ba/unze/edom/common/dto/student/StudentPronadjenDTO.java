package ba.unze.edom.common.dto.student;

/** Rezultat pretrage po JMBG-u: da admin ne unosi ponovo studenta koji već postoji. */
public record StudentPronadjenDTO(
        Integer idStudent,
        StudentUnosDTO podaci,
        boolean imaNalog
) {}