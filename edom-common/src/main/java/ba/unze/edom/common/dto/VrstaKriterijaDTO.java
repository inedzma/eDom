package ba.unze.edom.common.dto;

public record VrstaKriterijaDTO(
        Integer id,
        String sifra,
        String naziv,
        String grupa,
        boolean traziParametar,
        String nazivParametra,

        Integer idDokaza,
        String nazivDokaza
) {}