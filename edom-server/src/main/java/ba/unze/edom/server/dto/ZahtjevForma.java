package ba.unze.edom.server.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Forma "Zahtjev za promjenu sobe" na webu (poslije dodjele sobe). */
@Getter
@Setter
public class ZahtjevForma {

    /** ZAMJENA ili PREMJESTAJ. */
    private String vrsta = "ZAMJENA";

    @Size(max = 100)
    private String saStudentom;

    private Integer zeljenaSoba;

    @Size(max = 500)
    private String razlog;
}