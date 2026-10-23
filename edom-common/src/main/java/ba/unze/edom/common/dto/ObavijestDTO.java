package ba.unze.edom.common.dto;

import java.time.LocalDate;

/** Jedna obavijest za admina. */
public record ObavijestDTO(
        String vrsta,        // NOVA_PRIJAVA, BEZ_SOBE, ZAHTJEV_SOBA (kasnije i drugi zahtjevi)
        String kljuc,        // jedinstven, npr. "PRIJAVA:42" — po njemu desktop pamti šta je viđeno
        String naslov,
        String opis,
        LocalDate datum,     // može biti null
        Integer idPrijava,   // za otvaranje detalja; null ako nije vezano za jednu prijavu
        String upozorenje    // null = sve u redu
) {}