package ba.unze.edom.common.dto;

import java.util.List;

/**
 * Spisak dokumenata za prijavu koja se unosi na desktopu.
 * Pravi ga isti servis kao spisak na webu, pa su pravila na jednom mjestu.
 */
public record SpisakUnosDTO(
        List<Zahtjev> obavezni,
        List<Zahtjev> dodatni,
        List<Kriterij> kriterijiBezDokaza
) {

    /**
     * Jedan red spiska.
     *
     * @param potrebno       koliko dokumenata treba (po članu: broj ostalih članova)
     * @param poClanu        dokument se vodi za svakog člana domaćinstva posebno
     * @param sifreKriterija za dodatne: kriteriji koje ovaj dokument dokazuje
     */
    public record Zahtjev(
            String kljuc,
            String naziv,
            String opis,
            List<Vrsta> vrste,
            int potrebno,
            boolean poClanu,
            List<String> sifreKriterija
    ) {}

    public record Vrsta(Integer id, String naziv) {}

    public record Kriterij(String sifra, String naziv) {}
}