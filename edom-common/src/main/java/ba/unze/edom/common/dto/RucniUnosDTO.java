package ba.unze.edom.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RucniUnosDTO(
        @Valid @NotNull StudentUnosDTO student,
        @Valid @NotNull PrijavaUnosDTO prijava,
        List<@Valid KriterijUnosDTO> kriteriji,
        List<@Valid DokumentUnosDTO> dokumenti
) {}