package ba.unze.edom.common.dto;

import java.math.BigDecimal;

/** Jedan kriterij u Postavkama (tabela vrsta_kriterija). */
public record KriterijPostavkaDTO(
        Integer id,                 // null = novi
        String sifra,               // pravi je server, ne mijenja se
        String naziv,
        BigDecimal bodovi,          // null samo kod kriterija koji se računaju formulom
        String grupa,               // BRANIOCI ili DODATNI
        boolean traziParametar,
        String nazivParametra,
        boolean traziPeriod,
        boolean aktivan,
        Integer idDokaz,
        String dokaz,
        long brojPrijava,
        boolean racunaSe            // RVI, invalidnost roditelja, OS RBiH
) {
    public KriterijPostavkaDTO saAktivan(boolean a) {
        return new KriterijPostavkaDTO(id, sifra, naziv, bodovi, grupa, traziParametar, nazivParametra,
                traziPeriod, a, idDokaz, dokaz, brojPrijava, racunaSe);
    }
}