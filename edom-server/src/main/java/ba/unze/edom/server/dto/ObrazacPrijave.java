package ba.unze.edom.server.dto;

import java.util.List;

/** Podaci za prijavni obrazac koji student štampa, potpisuje i šalje poštom. */
public record ObrazacPrijave(
        String sifra,
        String akademskaGodina,      // "2026/2027"
        String imeOcaPrezime,
        String fakultetGodina,
        String telefon,
        String boraviste,
        String roditelj,
        String email,
        String jmbgLk,
        List<Pitanje> pitanja
) {
    public record Pitanje(int broj, String tekst, String odgovor) {}
}