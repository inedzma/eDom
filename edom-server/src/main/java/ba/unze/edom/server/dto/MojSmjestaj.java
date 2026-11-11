package ba.unze.edom.server.dto;

import java.time.LocalDate;
import java.util.List;

/** Podaci za stranicu "Moj smještaj". */
public record MojSmjestaj(
        Integer akademskaGodina,
        String statusPrijave,
        boolean primljen,
        Soba soba,                 // null dok soba nije dodijeljena
        boolean zeljaSacuvana
) {
    public record Soba(Integer broj, String sprat, Integer kapacitet,
                       boolean uselio, LocalDate datumUseljenja, List<Cimer> cimeri) {}

    public record Cimer(String ime, String telefon, String email) {}
}