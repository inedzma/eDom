package ba.unze.edom.common.dto.dokumenti;

import java.util.List;

/** Spisak dokumenata za admina: isti kao na webu, s priloženim/dostavljenim dokumentima. */
public record DokumentiSpisakDTO(
        List<Zahtjev> obavezni,
        List<Zahtjev> dodatni,
        List<String> kriterijiBezDokaza,
        List<DokumentDTO> ostali
) {
    public record Zahtjev(
            String kljuc,
            String naziv,
            String opis,
            boolean obavezan,
            int potrebno,          // npr. broj članova domaćinstva
            boolean poClanu,
            List<Vrsta> vrste,
            List<DokumentDTO> prilozeni
    ) {}

    public record Vrsta(Integer id, String naziv) {}
}