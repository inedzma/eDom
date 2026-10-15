package ba.unze.edom.common.dto;

public record RucniUnosRezultatDTO(
        Integer idPrijava,
        Integer idStudent,
        boolean noviNalog,
        double potvrdjeniBodovi,
        double potencijalniBodovi
) {}