package ba.unze.edom.common.dto.prijava;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Desktop pita server koji dokumenti trebaju za prijavu koja se tek unosi. */
public record SpisakUnosZahtjevDTO(
        @NotNull Integer godinaStudija,
        @NotNull Integer brojClanova,
        List<String> sifreKriterija
) {}