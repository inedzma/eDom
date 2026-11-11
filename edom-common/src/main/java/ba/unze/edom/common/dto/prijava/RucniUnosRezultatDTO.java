package ba.unze.edom.common.dto.prijava;

/** Odgovor servera nakon ručnog unosa papirne prijave. */
public record RucniUnosRezultatDTO(
        Integer idPrijava,
        Integer idStudent,
        boolean noviNalog,
        double bodovi
) {}