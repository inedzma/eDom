package ba.unze.edom.common.dto.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaNapomenaDTO(@NotBlank @Size(max = 1000) String tekst) {}