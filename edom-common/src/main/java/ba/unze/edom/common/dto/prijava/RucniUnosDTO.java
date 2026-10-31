package ba.unze.edom.common.dto.prijava;

import ba.unze.edom.common.dto.dokumenti.DokumentUnosDTO;
import ba.unze.edom.common.dto.student.StudentUnosDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RucniUnosDTO(
        @Valid @NotNull StudentUnosDTO student,
        @Valid @NotNull PrijavaUnosDTO prijava,
        List<@Valid KriterijUnosDTO> kriteriji,
        List<@Valid DokumentUnosDTO> dokumenti
) {}