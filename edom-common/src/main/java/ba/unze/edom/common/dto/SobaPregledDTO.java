package ba.unze.edom.common.dto;

import java.util.List;

/**
 * Jedna soba na pregledu.
 * spol: 'M' ili 'Z' (iz sobe, pa iz sprata, pa iz stanara); null = svejedno.
 * zaStudente: false za sobe za goste.
 */
public record SobaPregledDTO(
        Integer idSoba,
        int broj,
        String sprat,
        int redoslijedSprata,
        int kapacitet,
        String spol,
        String kategorija,
        String bojaKategorije,
        boolean zaStudente,
        boolean aktivna,
        int brojKljuceva,
        String napomena,
        List<StanarDTO> stanari
) {
    public int zauzeto() {
        return stanari == null ? 0 : stanari.size();
    }

    public int slobodno() {
        return Math.max(0, kapacitet - zauzeto());
    }
}