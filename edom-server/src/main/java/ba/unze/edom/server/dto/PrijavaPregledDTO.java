package ba.unze.edom.server.dto;

import java.time.LocalDate;

public record PrijavaPregledDTO(
        Integer idPrijava,
        Integer akademskaGodina,
        LocalDate datumPrijave,
        String status,
        Double ukupniBodovi,
        int brojDokumenata
) {}