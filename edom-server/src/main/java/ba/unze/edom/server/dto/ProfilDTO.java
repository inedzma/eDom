package ba.unze.edom.server.dto;

public record ProfilDTO(
        String ime,
        String prezime,
        String jmbg,
        String brojIndeksa,
        String imeRoditelja,
        String email,
        String telefon,
        String adresa,
        String fakultet,
        Integer godinaStudija
) {}