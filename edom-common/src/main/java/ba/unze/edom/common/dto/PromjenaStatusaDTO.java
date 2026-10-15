package ba.unze.edom.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromjenaStatusaDTO(
        @NotBlank(message = "Odaberite status.") String status,
        @Size(max = 500, message = "Napomena može imati najviše 500 znakova.") String napomena
) {}