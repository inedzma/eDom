package ba.unze.edom.server.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Sve za stranicu "Moj smještaj". soba = null dok soba nije dodijeljena. */
public record MojSmjestaj(
        Integer akademskaGodina,
        String statusPrijave,
        boolean primljen,
        Soba soba,
        boolean zeljaSacuvana,
        List<Zahtjev> zahtjevi,
        boolean imaZahtjevNaCekanju
) {

    public record Soba(int broj, String sprat, int kapacitet, boolean uselio,
                       LocalDate datumUseljenja, List<Cimer> cimeri) {}

    public record Cimer(String ime, String telefon, String email) {}

    public record Zahtjev(Integer id, String vrsta, String opis, String razlog, String status,
                          LocalDateTime datum, String odgovor) {

        public boolean naCekanju() {
            return "NA_CEKANJU".equals(status);
        }

        public String statusTekst() {
            return switch (status) {
                case "NA_CEKANJU" -> "Na čekanju";
                case "ODOBREN" -> "Odobren";
                case "ODBIJEN" -> "Odbijen";
                case "POVUCEN" -> "Povučen";
                default -> status;
            };
        }
    }
}