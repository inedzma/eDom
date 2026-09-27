package ba.unze.edom.server.dto;

public record RangStavkaDTO(
        Integer idStudenta,
        String prezime,
        String imeOca,
        String ime,
        Double bodovi
) {}