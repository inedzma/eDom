package ba.unze.edom.common.dto;

import java.time.LocalDateTime;

/**
 * Zahtjev za promjenu sobe, za pregled na desktopu.
 * drugiPodnio: samo za zamjenu, da li je i drugi student podnio zahtjev (samo informacija za admina).
 */
public record ZahtjevSobeDTO(
        Integer id,
        Integer idPrijava,
        String imePrezime,
        String brojIndeksa,
        Integer trenutnaSoba,
        String vrsta,
        String saStudentom,
        String saStudentomIme,
        Integer saStudentomSoba,
        Integer zeljenaSoba,
        String razlog,
        String status,
        LocalDateTime datum,
        String odgovor,
        Boolean drugiPodnio
) {}