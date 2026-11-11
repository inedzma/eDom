package ba.unze.edom.common.dto.prijava;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Kriterij pri ručnom unosu papirne prijave (desktop). */
public record KriterijUnosDTO(
        @NotBlank String sifra,
        BigDecimal parametar
) {}