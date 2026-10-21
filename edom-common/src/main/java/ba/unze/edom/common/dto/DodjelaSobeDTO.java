package ba.unze.edom.common.dto;

import jakarta.validation.constraints.NotNull;

/** Ručna dodjela ili premještaj: prijava ide u sobu. */
public record DodjelaSobeDTO(
        @NotNull Integer idPrijava,
        @NotNull Integer idSoba
) {}