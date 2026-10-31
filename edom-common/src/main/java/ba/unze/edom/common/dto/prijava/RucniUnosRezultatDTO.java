package ba.unze.edom.common.dto.prijava;

public record RucniUnosRezultatDTO(
        Integer idPrijava,
        Integer idStudent,
        boolean noviNalog,
        double potvrdjeniBodovi,
        double potencijalniBodovi
) {}