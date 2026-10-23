package ba.unze.edom.common.dto;

/** Poruka studenta administraciji (kontakt forma). */
public record PorukaDTO(
        String id,
        String ime,        // ime i prezime studenta
        String email,
        String sifra,      // SC-00001
        String naslov,
        String tekst,
        String vrijeme     // "23.10.2026 11:45"
) {}