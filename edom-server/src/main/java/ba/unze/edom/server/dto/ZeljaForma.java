package ba.unze.edom.server.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Forma "Želje za smještaj" na webu (prije dodjele sobe). */
@Getter
@Setter
public class ZeljaForma {

    /** 2 = dvokrevetna, 3 = trokrevetna, null = svejedno. */
    private Integer zeljeniKapacitet;

    /** Broj sobe, nije obavezno. */
    private Integer zeljenaSoba;

    @Size(max = 100)
    private String cimer1;

    @Size(max = 100)
    private String cimer2;

    @Size(max = 255)
    private String napomena;
}