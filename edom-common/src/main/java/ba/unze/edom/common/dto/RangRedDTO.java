package ba.unze.edom.common.dto;

public record RangRedDTO(
        int redniBroj,
        Integer idPrijava,
        Integer idStudent,
        String prezime,
        String imeOca,
        String ime,
        Integer godinaStudija,

        double uspjeh,
        double udaljenost,
        double primanja,
        double branioci,
        double dodatni,
        double ukupno,

        String greska
) {}