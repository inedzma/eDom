package ba.unze.edom.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Nepročitana poruka studenta; čuva se u fajlu dok je admin ne označi kao pročitanu. */
public record PorukaStudenta(
        String id,
        Integer idStudent,
        String ime,
        String email,
        String sifra,
        String naslov,
        String tekst,
        LocalDateTime vrijeme
) implements Serializable {}