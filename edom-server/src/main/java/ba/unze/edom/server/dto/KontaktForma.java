package ba.unze.edom.server.dto;

import java.io.Serializable;

/** Podaci iz forme na stranici Kontakt. "web" je zamka za botove (ljudi ga ne vide). */
public record KontaktForma(String ime, String email, String tema, String poruka, String web)
        implements Serializable {}