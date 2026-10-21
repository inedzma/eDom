package ba.unze.edom.common.dto;

public record RangRedDTO(
        int redniBroj,
        Integer idPrijava,
        Integer idStudent,
        String sifra,              // privremeno = ID prijave
        String prezime,
        String imeOca,
        String ime,
        Integer godinaStudija,
        KategorijaRanga kategorija,

        // grupe kao u Excelu doma
        double uspjeh,
        double branioci,
        double socijalni,          // primanja + izbjeglo lice + brat/sestra
        double udaljenost,
        double ukupno,

        boolean primljen,          // iznad crte (računa server)
        String dokazUslova,        // "21.10.2026 14:30" ili null
        boolean dokazNaknadno,     // dokaz evidentirao admin poslije predaje
        String greska
) {
    /** Kopija s rangom i oznakom primljen — koristi ga server pri raspodjeli. */
    public RangRedDTO saRangom(int rb, boolean jePrimljen) {
        return new RangRedDTO(rb, idPrijava, idStudent, sifra, prezime, imeOca, ime, godinaStudija,
                kategorija, uspjeh, branioci, socijalni, udaljenost, ukupno,
                jePrimljen, dokazUslova, dokazNaknadno, greska);
    }
}