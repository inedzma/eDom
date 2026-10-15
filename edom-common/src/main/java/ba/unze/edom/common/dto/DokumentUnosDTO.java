package ba.unze.edom.common.dto;

import jakarta.validation.constraints.NotNull;

public record DokumentUnosDTO(
        @NotNull Integer idVrste,
        String naziv,          // npr. "Potvrda o primanjima - otac"; null = naziv vrste
        boolean dostavljen,
        String b64             // opcionalno, ako je dokument skeniran
) {}